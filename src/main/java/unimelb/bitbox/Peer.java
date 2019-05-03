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
	
	/**
	 * serverPort is the advertised port of
	 * this peer. Other peers will use it to
	 * connect to it. It is not supposed to
	 * change once read from configuration file
	 */
	static final int serverPort = 
			Integer.parseInt(Configuration.getConfigurationValue("port"));

	static LinkedBlockingQueue<Message> incomingMessagesQ;
	
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Skeleton code - Begin
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        

        //Instantiate a common incoming Queue to be used by the network 
        // (this is a common one that is used by all threads [producer - consumer model])
        incomingMessagesQ = new LinkedBlockingQueue<>();
        HostPort serverHostPort = new HostPort(serverName,serverPort);
        
        //Also connection manager is common (Will maintain active connections established by either client or server part of the peer).
        ConnectionManager connectionManager = new ConnectionManager(maximumIncommingConnections, serverHostPort, incomingMessagesQ);
        
        
        //Start server component thread
        Thread serverThread=new Thread(new ServerMain(connectionManager));
        serverThread.start();
    	
        //Start client component thread
        new ClientMain(connectionManager);

        
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
	        								if(connectionManager.activePeerConnection.size()>0)
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
