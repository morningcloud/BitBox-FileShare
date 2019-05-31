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
import java.util.HashMap;
import java.util.logging.Logger;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Crypto;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.OpenSSHToRSAPubKeyConverter;
import unimelb.bitbox.util.Constants.PeerSource;

public class AuthenticationServer extends Thread {
	
	private int authenticationServerPort; //clientPort in configurationFile
	HashMap<String,Key> authorisedKeys;
	Crypto crypto;
	ConnectionManager connectionManager;
	private static Logger log = Logger.getLogger(AuthenticationServer.class.getName());
	private String serverName;
	HostPort authenticationServerHostPort;
	ServerSocket authenticationServerSocket=null;
	
	
	/**
	 * Reads authorized_keys property, converts keys to PKCS#8 that is readable by java
	 * and creates corresponding Key objects, load them in HashMap along with the key's identity  
	 */
	public AuthenticationServer(ConnectionManager connectionManager) throws NumberFormatException, IOException  
	{
		
		
		this.serverName = Configuration.getConfigurationValue("advertisedName");
		this.authenticationServerPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		authenticationServerHostPort = new HostPort(this.serverName,this.authenticationServerPort);
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
			this.authenticationServerSocket = new ServerSocket(this.authenticationServerPort);
			log.info("Authentication Server started, listening at "+authenticationServerPort);
			while (true)
			{
				clientSocket = this.authenticationServerSocket.accept();
				//TODO remove comment from the following line, reinstate socket timeout
				clientSocket.setSoTimeout(Constants.BITBOX_INCOMING_SOCKET_TIMEOUT);
				/**
				 * The following block validates the HANDSHAKE_REQUEST and checks the server's capacity.
				 */
				BufferedReader in;
				BufferedWriter out; 
				try 
				{
					
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));
					out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));					
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
								this.crypto  = new Crypto(idPubKey,Constants.CryptoUser.PEER);
								String encodedKey = this.crypto.getEncodedSessionKey();
								
								String[] args = {encodedKey,"true"};
								String responseMsg = Protocol.createMessage(Constants.Command.AUTH_RESPONSE, args);
								log.info(responseMsg);
								out.write(responseMsg);
								out.flush();
								
								/**
								 * The following part starts using secure communication, all JSON messages are encrypted
								 * and wrapped in "payload".
								 */
								//Receiving client request.
								inmsg = in.readLine();
								Document clientRequest = Document.parse(inmsg);
								Document reply = processClientRequest(clientRequest);
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
				if (this.authenticationServerSocket!=null) this.authenticationServerSocket.close();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}

	}
	
	private Document processClientRequest(Document clientRequest)
	{
		String encryptedRequest = clientRequest.getString("payload");
		String decryptedRequest = this.crypto.decrypt(encryptedRequest);
		Document clientReqDocument = Document.parse(decryptedRequest);
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
			//  * args[0] --> hostName
			//  * args[1] --> port
			//  * args[2] --> status
			String host = clientReqDocument.getString("host");
			int port = (int)clientReqDocument.getLong("port");
			HostPort peer = new HostPort(host,(int)port);
			//Calling some method to connect peer and return status
			
			
			boolean reqStatus = this.connectionManager.connectPeer(peer);
			
			String status = (reqStatus==true)?"true":"false";
			
			String[] responseArgs = {host,Integer.toString(port),status};
			response = Document.parse(Protocol.createMessage(Constants.Command.CONNECT_PEER_RESPONSE, responseArgs));
			
			break;
			
		}
		case "DISCONNECT_PEER_REQUEST":
		{
			
			String host = clientReqDocument.getString("host");
			int port = (int)clientReqDocument.getLong("port");
			HostPort peer = new HostPort(host,(int)port);
			//Calling some method to connect peer and return status
			
			
			boolean reqStatus = this.connectionManager.disconnectPeer(peer);
			
			String status = (reqStatus==true)?"true":"false";
			
			String[] responseArgs = {host,Integer.toString(port),status};
			response = Document.parse(Protocol.createMessage(Constants.Command.DISCONNECT_PEER_RESPONSE, responseArgs));

			break;
			
		}
		
		}
		String encryptedResponse = this.crypto.encrypt(response.toJson());
		Document payload = new Document();
		payload.append("payload", encryptedResponse);
		return payload;
	}

}
