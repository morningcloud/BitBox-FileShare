package unimelb.bitbox;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import unimelb.bitbox.Connection;
import unimelb.bitbox.util.Configuration;
import java.lang.Math;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.HostPort;

/**
 * This class will send a message to
 * the specified address. Note that
 * message sending must not be 
 * done inside a thread, otherwise the
 * receiver will start expecting response
 * before sending the message completely. 
 *
 */
public class UDPSend {
	
		private static Logger log = Logger.getLogger(UDPSend.class.getName());

		
		// To ensure minimum of 8192 Bytes or Block Size
		// is transferred as message to avoid packet sizes
		// greater than maximum packet size. 
		private static int MAX_UDP_PAYLOAD_SIZE =  65507;
		
			public synchronized void sendBitBoxMessage(HostPort hostPort, String bitBoxMessage) {
				InetAddress iPAddress;
				try {
					iPAddress = InetAddress.getByName(hostPort.host);
					sendBitBoxMessage(iPAddress, hostPort.port, bitBoxMessage);
				} catch (UnknownHostException e) {
					log.severe("UDPSend: Failed to Convert From HostPort to InetADdress " + e );
					e.printStackTrace();
				}
				
				
			}
		   public synchronized void sendBitBoxMessage(InetAddress IPOfPeer, int portOfPeer, String bitBoxMessage) {
			   
			     if (UDPPortProducer.serverSocketUDP!=null) {
			    	 try {

				    	   
					        DatagramSocket socket = UDPPortProducer.serverSocketUDP;
 
					        byte[] messageByteArray = bitBoxMessage.getBytes(Charset.forName("UTF-8"));
					        //System.out.println("MssageByteArray is: " + messageByteArray + " Length of MessaByteArray "+messageByteArray.length );
			
			
					        /*
					         * Even though it is highly unlikely that
					         * a bitBoxMessage reaches a length
					         * beyond Maximum_UDP_Payload_Size (65507Bytes
					         * as imposed by IP Packet)
					         * the below for loop is added to ensure that
					         * such cases are catered as worst cases, and
					         * send the message as more than one Datagram
					         * if such a situation arises.
					         * 
					         */
					        for (int datagramNumber=0; datagramNumber < messageByteArray.length; datagramNumber = datagramNumber+ MAX_UDP_PAYLOAD_SIZE  ) {
					            // This block from the entire bitBoxMessage
					            // will be sent during this iteration.
					        	
					            byte[] datagramToSend = new byte[Math.min(messageByteArray.length, MAX_UDP_PAYLOAD_SIZE)];// not to be confused with given block size spec					            
					            for(int j=0; j<datagramToSend.length; j++ ) {
					            	datagramToSend[j] = messageByteArray[datagramNumber+j];
					            	//System.out.println(blockToSend[j]+ " "+ messageByteArray[blockNumber+j]);
					            }
					            
					            
					            //System.out.println("Block To Send Is: "+ blockToSend + " Length of BlockToSend "+blockToSend.length );
					            // Send the message
					            DatagramPacket block = new DatagramPacket(datagramToSend, datagramToSend.length, IPOfPeer, portOfPeer);
					            socket.send(block);
					            //log.info("Block Sent as bytes : " + blockToSend);
					            log.info("Block Sent as String : " + new String(datagramToSend) + " BlockNumber: " + datagramNumber);
					        }
		
				        //log.info(this.getClass().getName()+bitBoxMessage + ": has been sent as Complete");
		
				    }catch(Exception e) {
				    	log.info(this.getClass().getName()+"Exception in UDPSend Class: " + e);
				    	e.printStackTrace();
				    }
			     }
				
			}

	    	

	    
/*
	    public void sendBitBoxMessage(String IPOfPeer, int portOfPeer, String bitBoxMessage) {
	        	ExecutorService service = Executors.newSingleThreadExecutor();
	             UDPSendRunnable task = new UDPSendRunnable(IPOfPeer, portOfPeer,bitBoxMessage);
	             service.execute(task);
	             service.shutdown();
	    	

	    }
	    
	    
	    class UDPSendRunnable implements Runnable{
	    	private String IPOfPeer;
	    	private int portOfPeer;
	    	private String bitBoxMessage;
	    	
	    	public UDPSendRunnable(String IPOfPeer, int portOfPeer, String bitBoxMessage) {
	    		this.IPOfPeer = IPOfPeer;
	    		this.portOfPeer=portOfPeer;
	    		this.bitBoxMessage = bitBoxMessage;
	    		
	    	}

			@Override
			public void run() {
			     if (UDPPortListener.serverSocketUDP!=null) {
			    	 try {
				    	   log.info(this.getClass().getName()+ ": Message : " + bitBoxMessage);
				    	   log.info(this.getClass().getName()+": Above message is being sent to : " + IPOfPeer + " " + portOfPeer);
			
					        DatagramSocket socket = UDPPortListener.serverSocketUDP;
					        log.info("Successfully accessed UDP Socket from UDPServerClass");
					        InetAddress address = InetAddress.getByName(IPOfPeer);
					        //The message is being converted to byte[] 
					        byte[] messageByteArray = bitBoxMessage.getBytes(Charset.forName("UTF-8"));
			
			
					        // Each Iteration of this for loop process
					        // number of bytes in original message equal to
					        // block size of bitBoxMessage minus Header length.
					        for (int blockNumber=0; blockNumber < messageByteArray.length; blockNumber = blockNumber+ block_size  ) {
					            // This block from the entire bitBoxMessage
					            // will be sent during this iteration.
					            byte[] blockToSend = new byte[block_size];
					            // Send the message
					            DatagramPacket block = new DatagramPacket(blockToSend, blockToSend.length, address, portOfPeer);
					            socket.send(block);
					            log.info("Block Sent as bytes : " + blockToSend);
					            log.info("Block Sent as String : " + new String(blockToSend));
					        }
		
				        log.info(this.getClass().getName()+bitBoxMessage + ": has been sent as Complete");
		
				    }catch(Exception e) {
				    	log.info(this.getClass().getName()+"Exception in UDPSend Class: " + e);
				    }
			     }
				
			}
	    	
	    }
	    
	 */   
}
