package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.net.Socket;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import java.net.*;
import java.io.*;

public class Peer 
{
    
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private ArrayList<HostPort> peerList = new ArrayList<HostPort>();
    DataInputStream in;
    DataOutputStream out;
	public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Skeleton code - Begin
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        String serverName = Configuration.getConfigurationValue("advertisedName");
        String serverPort = Configuration.getConfigurationValue("port");
        Peer agent = new Peer();
        agent.parsePeerEntry(Configuration.getConfigurationValue("peers"));
        System.out.println("Peers list:");
    	System.out.println("-----------");
        for (HostPort h: agent.peerList) System.out.printf("%s:%s\n",h.host,h.port);
        agent.connect();
        new ServerMain(serverName,serverPort);
        
    }
    /**
     * Processes peers entry in the configuration file, creates a HostPort list.
     * @param list
     */
    private void parsePeerEntry(String list)
    {
    	String[] hostList = list.split(",");
    	for (String h: hostList) this.peerList.add(new HostPort(h));
    }
    
    private void connect()
    {
    	Socket socket =null;
    	boolean connected = false;
    	boolean eol = false;
    	try
    	{
        	//Looping through the list of peers
    		while (!connected & !eol)
        	{
    			int peerIndex=0;
    			while (peerIndex<peerList.size() & !connected)
    			{
        			HostPort peer = peerList.get(peerIndex);
    				System.out.printf("Trying peer=%s:%s...",peer.host,peer.port);
    				try
    				{
        				socket = new Socket(peerList.get(peerIndex).host,peer.port);
            			if (socket.isConnected())
            			{
            				socket.setKeepAlive(true);
            				System.out.println("Connected!");
            				connected = true;
            			}
    				}
    				catch (ConnectException e)
    				{
    					System.out.println("Connection failed");
    					//log.warning("Connection failed");
    				}
    				peerIndex++;
    			}
    			eol = true;
        	}
    		
    		
    		if (connected)
    		{
    			in = new DataInputStream(socket.getInputStream());
    			out = new DataOutputStream(socket.getOutputStream());
    			//log.info("Sending data..");
    			System.out.println("Starting protocol..");
    			int msgCounter=0;
    			while (true)
    			{
        			System.out.println("Connection still alive.."+msgCounter);
        			Thread.sleep(3000);
        			/*
    				out.writeUTF("Test "+msgCounter);
        			//log.info("Receiving data..");
        			//log.info("Received:"+in.readUTF());
        			System.out.println("Receiving data..");
        			System.out.println("Received: "+in.readUTF());
        			
        			*/
        			msgCounter++;
    			}
    		}
    	}
    	
    	catch (Exception e)
    	{
    		System.out.println("Peer connection error:"+e.getMessage());
    	}
    	finally
    	{
    		if (socket!=null)
    		{
    			try 
    			{
    				socket.close();
    			} 
    			catch(IOException e) 
    			{
    				log.warning(e.getMessage());
    			}
    		}
    	}
    }
}
