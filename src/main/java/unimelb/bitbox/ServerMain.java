package unimelb.bitbox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	public TransportAgent transportAgent;
    int connectionCount = 0;
    int MAX_NO_OF_CONNECTION=5;
    ServerSocket serverSocket=null;
    
    private String serverName;
	private int serverPort;
	
	public ServerMain(String serverName, int serverPort) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		if (serverName!="" & serverPort!=-1)
		{
			this.serverName = serverName;
			this.serverPort = serverPort;
		}
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);		
		this.transportAgent = new TransportAgent();
		runServer();
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) 
	{
		// TODO: process events Check event type and process
		log.info(String.format("Event Raised. EventType: %s FileName: '%s' Path: '%s'", 
				fileSystemEvent.event.toString(), fileSystemEvent.name, fileSystemEvent.path));
	}
    
    public void runServer()
    {
		Socket clientSocket;
		try
		{
			this.serverSocket = new ServerSocket(this.serverPort);
			System.out.println("Server started, listening at "+serverPort);
			while (true)
			{
				clientSocket = this.serverSocket.accept();
				if (this.transportAgent.connectedPeers.size()<this.MAX_NO_OF_CONNECTION)
				{
					this.transportAgent.addConnection(clientSocket);
					log.info(String.format("Connected to: %s, total number of established connections: %s\n",
							clientSocket.getInetAddress().getHostName(),
							this.transportAgent.connectedPeers.size()
							));
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
			for (String peer: this.transportAgent.getPeerList())
			{
				log.info("Disconnecting form "+peer);
			}
				
		}
    }
}
