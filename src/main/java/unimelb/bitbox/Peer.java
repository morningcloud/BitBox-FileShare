package unimelb.bitbox;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import java.net.*;
import java.io.*;

public class Peer 
{
	
	//TODO For UDP, Server must start first and after that client
	
	private static Logger log = Logger.getLogger(Peer.class.getName());
	// private ArrayList<HostPort> peerList = new ArrayList<HostPort>();
	
	/**
	 * serverName is the advertised name of
	 * this peer. Other peers will use it to
	 * connect to it. It is not supposed to
	 * change once read from configuration file
	 */
	static final public String serverName = 
			Configuration.getConfigurationValue("advertisedName");

	static final int maximumIncommingConnections = 
			Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
	
	public static final String mode = Configuration.getConfigurationValue("mode").toLowerCase();
	public static DatagramSocket dSocket;
	
	/**
	 * serverPort is the advertised port of
	 * this peer. Other peers will use it to
	 * connect to it. It is not supposed to
	 * change once read from configuration file
	 */
	
	static final int serverPort = 
			Integer.parseInt(Configuration.getConfigurationValue("port"));
	
	static final int udpServerPort = 
			Integer.parseInt(Configuration.getConfigurationValue("udpPort"));

	static LinkedBlockingQueue<Message> incomingMessagesQ;
	public static HostPort serverHostPort;
	
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Skeleton code - Begin
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        

        //Instantiate a common incoming Queue to be used by the network 
        // (this is a common one that is used by all threads [producer - consumer model])
        incomingMessagesQ = new LinkedBlockingQueue<>();
        if (Peer.mode.equals("tcp")) {
        	serverHostPort = new HostPort(serverName,serverPort);
        	Peer.initializeComponents(serverHostPort);
        	
        }else if (Peer.mode.equals("udp")) {
        	serverHostPort = new HostPort(serverName,udpServerPort);  
        	Peer.initializeComponents(serverHostPort);
        }else {
        	log.info("Peer Class Says: Incorrect mode in Configuration File... Exitin the system");
        	System.exit(0);
        }
        	
    }  
        private static void initializeComponents(HostPort serverHostPort) {
            //Also connection manager is common (Will maintain active connections established by either client or server part of the peer).
            ConnectionManager connectionManager = new ConnectionManager(maximumIncommingConnections, serverHostPort, incomingMessagesQ);
           
            try {
            	new Thread(new AuthenticationServer(connectionManager), "Authentication Server Thread").start();
			} catch(BindException be) {
				log.severe("AuthorizationServer Port already in Use.....Exiting the system");
				System.exit(0);
				
			}
            catch (NumberFormatException | IOException e) {
				// TODO Check This
				e.printStackTrace();
			} 
            
            
            if(Peer.mode.equals("tcp")) {
                //Start server component thread
                Thread serverThread;
				try {
					serverThread = new Thread(new ServerMain(connectionManager));
					serverThread.start();
				} catch (NumberFormatException | IOException e) {
					// TODO Check This
					e.printStackTrace();
				}
                            	
                //Start client component thread
                new ClientMain(connectionManager);
            	
            }else if(Peer.mode.equals("udp")) {
            	
                //Start server component thread
                Thread producerThread=new Thread(new UDPPortProducer(), "UDPPortListener Thread");
                producerThread.start();
                
                new Thread(new UDPHandShakeServer(connectionManager), "UDPHanhdshakeServer Thread").start();
                
                //Start client component thread
                new UDPClient(connectionManager);
            	
            }


            
            //start the event processor
            EventProcessor eventProcess= new EventProcessor(connectionManager);
            Thread eventProcessor = new Thread(eventProcess);
            eventProcessor.start();
            
            
            long syncInterval = Long.parseLong(Configuration.getConfigurationValue("syncInterval")) * 1000; //get time in milli
            //start sync event timer tread
            //Works but may implement in a better way
            Thread syncThread = new Thread(new Runnable() {
            							public void run() {
            								while(true) {
    	        								log.info("Time for Sync");
    	        								if(connectionManager.activePeerHostPort.size()>0)
    	        									eventProcess.processSyncEvents();
    	        								try {
    												Thread.sleep(syncInterval);
    											} catch (InterruptedException e) {
    												// Ignore
    											}catch (Exception e) {
    												log.severe("Exception in SyncEvents Timer Thread");
    												e.printStackTrace();
    											}
            								}
            							}
            });
            syncThread.start();
            
        }
        	
}

		
   

