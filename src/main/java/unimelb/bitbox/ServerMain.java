package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import unimelb.bitbox.Err.*;
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
	HostPort serverHostPort;

	
	public ServerMain(ConnectionManager connectionManager) throws NumberFormatException, IOException  {
		
		this.serverName = Configuration.getConfigurationValue("advertisedName");
		this.serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		serverHostPort = new HostPort(this.serverName,this.serverPort);
		this.connectionManager = connectionManager;
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
					Document response;
					String inmsg = in.readLine();
					Document handshakeMsg = Document.parse(inmsg);
					try
					{
						//Validates the HANDSHAKE_REQUEST and throws exception if invalid.
						if (Protocol.validate(handshakeMsg))
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
								HostPort connectingPeer = new HostPort((Document)handshakeMsg.get("hostPort"));
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
								HostPort connectingPeer = new HostPort((Document)handshakeMsg.get("hostPort"));
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
		}
		catch (IOException ex) 
		{
			log.severe("IOException "+ex.getMessage());
			ex.printStackTrace();
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
}
