package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.KeySpec;

import unimelb.bitbox.Err.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;



import java.net.InetAddress;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.*;


public class ServerMain implements Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	public ConnectionManager connectionManager;
	final int SOCKET_TIMEOUT = 100000;	//Timeout for server socket, 3 seconds
    int connectionCount = 0;
    InetAddress serverAddress;
    ServerSocket serverSocket=null;
    
    //What is the point of having advertised name if we can't set it??
    private String serverName;
	private int serverPort;
	private int clientPort;
	HostPort serverHostPort;
	HashMap<String,Key> authorisedKeys;

	
	public ServerMain(ConnectionManager connectionManager) throws NumberFormatException, IOException  
	{
		
		this.serverName = Configuration.getConfigurationValue("advertisedName");
		this.clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		this.serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		serverHostPort = new HostPort(this.serverName,this.serverPort);
		this.connectionManager = connectionManager;
		loadAuthorisedKeys();
	}
	
	private void loadAuthorisedKeys()
	{
		Configuration.getConfiguration();
		HashMap<String,String> authKeys = Configuration.getAuthKeys();
		this.authorisedKeys = new HashMap<String,Key>();
		for (String identity: authKeys.keySet())
		{
			OpenSSHToRSAPubKeyConverter keyConverter = new OpenSSHToRSAPubKeyConverter(authKeys.get(identity).getBytes());
			try
			{
				KeySpec spec = keyConverter.convertToRSAPublicKey();
				KeyFactory kf = KeyFactory.getInstance("RSA");
				Key identityRSAKey = kf.generatePublic(spec);
				this.authorisedKeys.put(identity, identityRSAKey);
			}
			catch (Exception e)
			{
				log.warning("Key couldn't be converted..check key format");
			}
		}
	}


	@Override
	public void run()
	{
		Socket clientSocket=null;
		//BufferedReader in = null;
		//BufferedWriter out = null;
		try 
		{
			this.serverSocket = new ServerSocket(this.serverPort);
			log.info("Server started, listening at "+serverPort);
			while (true)
			{
				clientSocket = this.serverSocket.accept();
				//TODO remove comment from the following line, reinstate socket timeout
				//clientSocket.setSoTimeout(Constants.BITBOX_INCOMING_SOCKET_TIMEOUT);
				/**
				 * The following block validates the HANDSHAKE_REQUEST and checks the server's capacity.
				 */
				BufferedReader in;
				BufferedWriter out; 
				try 
				{
					
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));
					out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));
					Document response;
					String inmsg = in.readLine();
					
					//TODO Tarek: Change 1:  Checking on incoming message if it's coming from Peer or Client.
					Document receivedMsg = Document.parse(inmsg);
					try 
					{
						String command = receivedMsg.getString("command");
						if (command.equals("AUTH_REQUEST"))
						{
							String identity = receivedMsg.getString("identity");
							log.info(String.format("Client %s, identity: %s requesting authentication",clientSocket.getInetAddress().getHostName(),identity));
							Key idPubKey = this.authorisedKeys.get(identity);
							if (idPubKey==null)
							{
								log.info(String.format("Client %s key not found. Sending authentication response and closing socket.",identity));
								String[] args = {null,"false"};
								String responseMsg = Protocol.createMessage(Constants.Command.AUTH_RESPONSE, args);
								out.write(responseMsg);
								out.flush();
								//TODO should wait for the incoming request, fulfil it, then close socket.
								clientSocket.close();
							}
							else
							{
								log.info(String.format("Client %s key found..sending authentication response and waiting for client's command",identity));
								String[] args = {idPubKey.getFormat(),"true"};
								String responseMsg = Protocol.createMessage(Constants.Command.AUTH_RESPONSE, args);
								out.write(responseMsg);
								out.flush();
								
								//Receiving client request.
								inmsg = in.readLine();
								log.info("received request: "+inmsg);
								Document reply = processClientRequest(inmsg);
								log.info("sending reply: "+reply.toJson());
								out.write(reply.toJson()+"\n");
								out.flush();
								
								//TODO should wait for the incoming request, fulfil it, then close socket.
								clientSocket.close();
								
								//TODO the following
								//Generate secret key using AES128
								//Encrypt the secret key using the client's public key above
								//Encode all in base64
								//Call Protocol.createMessage(auth response).
								//Keep connection open awaiting for next command.
								//Disconnect as soon as the latter request is fulfilled.
								
							}
						}
						else
						{
							Document handshakeMsg = Document.parse(inmsg);
							try
							{
								HostPort connectingPeer = null;
								//Validates the HANDSHAKE_REQUEST and get the host of the peer... throws exception if invalid.
								connectingPeer = Protocol.validateHS(handshakeMsg);
								if (connectingPeer != null && connectionManager.isPeerConnected(connectingPeer)) {
									response = new Document();
									response.append("command", "INVALID_PROTOCOL");
									response.append("message", "Peer was already connected.");
									out.write(response.toJson()+"\n");
									out.flush();
									log.info(String.format("Connection from %s refused.\n Message sent: %s\n",connectingPeer.toString(),response.toJson()));
									clientSocket.close();
								}
								else if (connectingPeer != null) 
								{
									//If HANDSHAKE_REQUEST is valid but the server is at it's maximum incoming connection capacity
									//a CONNECTION_REFUSED message is sent and the connection is closed.
									if (connectionManager.getActiveConnectionCountBySource(PeerSource.SERVER) >= connectionManager.MAX_NO_OF_CONNECTION)
									{
										response = new Document();
										response.append("command", "CONNECTION_REFUSED");
										response.append("message", "connection limit reached");
										response.append("peers", connectionManager.getPeerList());
										out.write(response.toJson()+"\n");
										out.flush();
										log.info(String.format("Connection from %s refused.\n Message sent: %s\n",connectingPeer.toString(),response.toJson()));
										clientSocket.close();
										
									}
									//if the HANDSHAKE_REQUEST is valid and the server has capacity, the connection is
									//added to ConnectionManager, and a HANDSHAKE_RESPONSE message is sent to the connecting peer.
									else
									{
										response = new Document();
										response.append("command", "HANDSHAKE_RESPONSE");
										response.append("hostPort",this.serverHostPort.toDoc());
										out.write(response.toJson()+"\n");
										out.flush();
										this.connectionManager.addConnection(clientSocket, PeerSource.SERVER, connectingPeer);
										log.info(String.format("Connected to: %s, total number of established connections: %s\n",
												clientSocket.getInetAddress().getHostName(),
												this.connectionManager.activePeerConnection.size()
												));
									}
								}
								else //if something other than handshake received decline
								{
									response = new Document();
									response.append("command", "INVALID_PROTOCOL");
									response.append("message", "invalid message, expecting HANDSHAKE_REQUEST");
									out.write(response.toJson()+"\n");
									out.flush();
									clientSocket.close();
								}
							}

							//If HANDSHAKE_REQUEST is invalid, respond with INVALID_PROTOCOL and close connection
							catch (InvalidCommandException e)
							{
								response = new Document();
								response.append("command", "INVALID_PROTOCOL");
								response.append("message", e.getMessage());
								out.write(response.toJson()+"\n");
								out.flush();
								clientSocket.close();
							}
						}
					}
					catch (SocketTimeoutException e)
					{
						if (clientSocket!=null)
						{
							log.warning(String.format("Socket timeout while waiting for Handshake Request from %s dropping the connection.",
									clientSocket.getInetAddress().getHostName()));
							try {clientSocket.close();}
							catch (Exception z) {log.warning("Error while closing client socket: "+z.getMessage());}
						}
					}
					catch (IOException e)
					{
						log.severe(String.format("IO exception while connecting to %s",
								clientSocket.getInetAddress().getHostName()));
						e.printStackTrace();
					}
				}
			
			catch (IOException ex) 
			{
				log.severe("IOException "+ex.getMessage());
				ex.printStackTrace();
			}
				
		
					
					//TODO End of Change 1
		}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			try
			{
				if (this.serverSocket!=null) this.serverSocket.close();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}

	}
	
	private Document processClientRequest(String clientRequest)
	{
		Document clientReqDocument = Document.parse(clientRequest);
		Document response = new Document();
		String command = clientReqDocument.getString("command");
		switch (command)
		{
		case "LIST_PEERS_REQUEST":
		{
			//TODO get connected peer list from connectionManager
			//What do we do for UDP? we don't have connected peers in that sense.

			response.append("command", "LIST_PEERS_RESPONSE");
			response.append("peers", connectionManager.getPeerList());
			break;
		}
		case "CONNECT_PEER_REQUEST":
		{
			//TODO Add code to extract HostPort of the peer from message, then try to connect to it using Peer (by passing a message).
			response.append("command", "CONNECT_PEER_RESPONSE");
			response.append("host", "someHost");
			response.append("port", 9000);
			response.append("status", true);	//TODO should be the result of the connection request to this host
			response.append("message", "connected to peer"); //TODO or connection failed, based on the result of the connection.	
			break;
			
		}
		case "DISCONNECT_PEER_REQUEST":
		{
			response.append("command", "DISCONNECT_PEER_RESPONSE");
			response.append("host", "someHost");
			response.append("port", 9000);
			response.append("status", true);	//TODO should be the result of the connection request to this host
			response.append("message", "connected to peer"); //TODO or connection failed, based on the result of the connection.	
			break;
			
		}
		
		}
		return response;
			
	}
}
