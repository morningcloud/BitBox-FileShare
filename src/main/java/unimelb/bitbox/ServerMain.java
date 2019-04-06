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

    int portNumber = 9831;
    int connectionCount = 0;
    int MAX_NO_OF_CONNECTION=5;
    ServerSocket serverSocket=null;
    private String serverName;
	private int serverPort;
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		runServer();
		
	}
	
	public ServerMain(String serverName, String serverPort) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		if (serverName!="" & serverPort!="")
		{
			this.serverName = serverName;
			this.serverPort = Integer.parseInt(serverPort);
		}
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);		
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events Check event type and process

		log.info(String.format("Event Raised. EventType: %s FileName: '%s' Path: '%s'", fileSystemEvent.event.toString(), fileSystemEvent.name, fileSystemEvent.path));
	}
    
	//BELOW IS A DUMMY CODE
    public void runServer(){
        try {
            serverSocket = new ServerSocket(portNumber);
            serverSocket.setReuseAddress(true); //to be able to rerun the program on the same port directly as it may not directly get released
            
            while(true){
                if (connectionCount < MAX_NO_OF_CONNECTION){
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New Client Connection established. "+clientSocket.getInetAddress().getHostAddress());
                    Thread th= new Thread(new ServerRunnable(clientSocket));
                    th.start();
                    connectionCount++;
                    System.out.println("Active Connections: "+connectionCount);
                }
            }
            
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        
    }
	
}
