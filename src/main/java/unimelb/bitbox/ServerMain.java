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

import unimelb.bitbox.*;
import unimelb.bitbox.util.*;
import java.util.Stack;
import java.util.LinkedList;

public class ServerMain implements FileSystemObserver {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	public TransportAgent transportAgent;
    int connectionCount = 0;
    
    ServerSocket serverSocket=null;
    
    private String serverName;
	private int serverPort;

	
	public ServerMain(String serverName, int serverPort) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		if (serverName!="" & serverPort!=-1)
		{
			this.serverName = serverName;
			this.serverPort = serverPort;
		}
		Configuration.getConfiguration();
		int maxInboundConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);		
		//this.transportAgent = new TransportAgent(maxInboundConnections);
		//runServer();
		ProtocolDriver pd = new ProtocolDriver();
		//processExeternalEvent(Protocol.EVENT.FILE_CREATE_REQUEST,pd.TestCreateFile());
		processExeternalEvent(Protocol.EVENT.DIRECTORY_CREATE_REQUEST, pd.TestCreateDir());
	}

	public Protocol.EVENT processExeternalEvent(Protocol.EVENT event, Document doc)
	{
		Protocol.EVENT response = null;
		
		if (event==Protocol.EVENT.FILE_CREATE_REQUEST)
		{
			String pathName = doc.getString("pathName");
			Document fileDescriptor = (Document)doc.get("fileDescriptor");
			String md5 = fileDescriptor.getString("md5");
					
			long fileSize = fileDescriptor.getLong("fileSize");
			long lastModified = fileDescriptor.getLong("lastModified");
			
			try
			{
				System.out.println("Actually calling file creator..");
				fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
			}
			
			catch (IOException e)
			{
				log.severe("IO error: "+e.getMessage());
			}
			catch (NoSuchAlgorithmException e)
			{
				log.severe("No such algorithm: "+e.getMessage());
			}
			catch (Exception e)
			{
				log.severe("Unhandled exception: "+e.getMessage());
			}
			
		}
		if (event==Protocol.EVENT.DIRECTORY_CREATE_REQUEST)
		{
			String pathName = doc.getString("pathName");
			if (fileSystemManager.isSafePathName(pathName))
			{
				LinkedList<String> dirs = new LinkedList<String>();
				//populate the Queue with directories separated by "/"
				for (String dir: pathName.split("/"))
				{
					dirs.add(dir);
				}
				try
				{
					boolean isCreated = true;
					String dir="";
					
					//Creating directories starting with leaves, stops if creating any sub-folder fails.
					while (!dirs.isEmpty()&isCreated)
					{
						dir += "/"+ dirs.pop();
						isCreated = fileSystemManager.makeDirectory(dir);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
			}
			else
			{
				System.out.println("Invalid path!");
			}
		}
		
		
		return response;
	}

	
	
	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) 
	{
		// TODO: process events Check event type and process
		/*
		log.info(String.format("Event Raised. EventType: %s FileName: '%s' Path: '%s'", 
				fileSystemEvent.event.toString(), fileSystemEvent.name, fileSystemEvent.path));
				*/
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
				this.transportAgent.addConnection(clientSocket);
				log.info(String.format("Connected to: %s, total number of established connections: %s\n",
						clientSocket.getInetAddress().getHostName(),
						this.transportAgent.connectedPeers.size()
						));
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
