package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Crypto;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.Constants.PeerSource;

/**
 * This class is acting as the producer.
 * It is continuously listening to incoming
 * DatagramPAckets to UDP port and adding
 * them to Queue.
 *
 */
public class UDPPortProducer extends Thread {
	public static DatagramSocket serverSocketUDP = null;
	public static int UDPListenerPort = 
			Integer.parseInt(Configuration.getConfigurationValue("udpport"));
	public static volatile BlockingQueue<DatagramPacket> udpPacketsQueue = new LinkedBlockingQueue<DatagramPacket>();
	private static final int block_size = 65507;//maximum UDP datagram size
	private static Logger log = Logger.getLogger(UDPPortProducer.class.getName());

	
	public void run(){		
		try {			
			
			serverSocketUDP = new DatagramSocket(UDPListenerPort);
			serverSocketUDP.setSoTimeout(0);
			log.info("UDPPortProducer has started listening at port: " + UDPListenerPort);	
		
			while(true) {			
					byte[] blockToReceive = new byte[block_size];
					DatagramPacket receivedPacket = new DatagramPacket(blockToReceive, blockToReceive.length);
					serverSocketUDP.receive(receivedPacket);
					boolean added = UDPPortProducer.udpPacketsQueue.add(receivedPacket);
					//System.out.println(added);
			}
		}catch(BindException be) {
			log.info("Failed to bind to UDP Port, Exiting the system: " + be);
			System.exit(0);			
			
		}
		catch (SocketException e) {
			log.info("UDPPortListener failed to Start: " + e);
		}
		catch (IOException s) {
			log.info("UDPPortListener failed to Start: " + s);		
		}
	}

		

}	