package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import unimelb.bitbox.Err.*;
import java.util.logging.Logger;
import java.net.InetAddress;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;


public class ServerMain implements Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	public ConnectionManager connectionManager;
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
		Socket clientSocket;
		//BufferedReader in = null;
		//BufferedWriter out = null;
		try 
		{
			this.serverSocket = new ServerSocket(this.serverPort);
			log.info("Server started, listening at "+serverPort);
			while (true)
			{
				clientSocket = this.serverSocket.accept();
				/**
				 * The following block validates the HANDSHAKE_REQUEST and checks the server's capacity.
				 */
				try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));
					 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));)
				{
					Document response;
					Document handshakeMsg = Document.parse(in.readLine());
					try
					{
						System.out.println("TE: validating received msg: "+handshakeMsg.toJson());
						//Validates the HANDSHAKE_REQUEST and throws exception if invalid.
						if (Protocol.validate(handshakeMsg))
						{
							System.out.println("TE: message is valid");
							//If HANDSHAKE_REQUEST is valid but the server is at it's maximum capacity
							//a CONNECTION_REFUSED message is sent and the connection is closed.
							if (connectionManager.connectedPeers.size()>=connectionManager.MAX_NO_OF_CONNECTION)
							{
								System.out.println("TE: message is valid, but max reached");  
								response = new Document();
								response.append("command", "CONNECTION_REFUSED");
								response.append("message", "connection limit reached");
								response.append("peers", connectionManager.getPeerList());										  ;
								out.write(response.toJson());
								out.flush();
								clientSocket.close();
							}
							//if the HANDSHAKE_REQUEST is valid and the server has capacity, the connection is
							//added to ConnectionManager, and a HANDSHAKE_RESPONSE message is sent to the connecting peer.
							else
							{
								System.out.println("TE: message is valid, all good adding this connection");  
								this.connectionManager.addConnection(clientSocket, PeerSource.SERVER);
								log.info(String.format("Connected to: %s, total number of established connections: %s\n",
										clientSocket.getInetAddress().getHostName(),
										this.connectionManager.connectedPeers.size()
										));
								response = new Document();
								response.append("command", "HANDSHAKE_RESPONSE");
								response.append("hostPort",this.serverHostPort.toDoc());
								out.write(response.toJson());
								out.flush();
								this.connectionManager.addConnection(clientSocket, PeerSource.SERVER);
							}
						}
					}

					//If HANDSHAKE_REQUEST is invalid, respond with INVALID_PROTOCOL and close connection
					catch (InvalidCommandException e)
					{
						System.out.println("TE: Captiromg InvalidCommand exception");  
						response = new Document();
						response.append("command", "INVALID_PROTOCOL");
						response.append("message", e.getMessage());
						out.write(response.toJson());
						out.flush();
						clientSocket.close();
					}
				}
				catch (IOException e)
				{
					log.warning(String.format("IO exception while connecting to %",
							clientSocket.getInetAddress().getHostName()));
				}
			}
		}
		catch (IOException ex) 
		{
			log.severe(ex.getMessage());
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
