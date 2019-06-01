/**
 * This class make use of the DatagramPackets
 * in the Queue specified in UDPServer to 
 * convert Datagrams into messages from a 
 * particular remote address.
 * It will timeout after the time specified
 * in TIME_OUT and return null 
 */


package unimelb.bitbox;

import java.io.*;
import java.lang.Math;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.HostPort;



public class UDPReceive {

	private static final int block_size = Constants.INSTANCE_BLOCK_SIZE;
	private static Logger log = Logger.getLogger(UDPReceive.class.getName());	
	int TIME_OUT = Constants.UDP_TIMEOUT;
	
	/* Because the UDP port for receiving Datagrams is common
	 * for all type of messages, it is important to identify
	 * the desired source. That's why we need to include
	 * the expected remote IP and remote Port while receiving the
	 * message.
	 */
	
	/**
	 * 
	 * @param expectedRemoteIP: The remote host from which message is expected
	 * @param expectedRemotePort: The remote port from which message is expected
	 * @return: The message in the form of String after its
	 *  compilation from Datagrams received from remote host.
	 */

	
		
	public String receiveBitBoxMessage(HostPort hostPort, Integer time_out) {

		String message = null;
		InetAddress iPAddress;
		try {
			iPAddress = InetAddress.getByName(hostPort.host);
			message = receiveBitBoxMessage(iPAddress, hostPort.port, time_out);
		} catch (UnknownHostException e) {
			log.severe("UDPReceive: Failed to Convert From HostPort to InetADdress " + e );
			e.printStackTrace();
			return message;
		}
		//System.out.println("3.(Optional) UDP Receive is returning: " + message);
		return message;		
	}
	
	
	public String receiveBitBoxMessage(InetAddress expectedRemoteIP, int expectedRemotePort, Integer time_out) {
		boolean isTimeOutRequired = true;
		
		//null means default, 0 means no time_out
		if (time_out != null) {//case of  a user_defined time out is required
			
			if (time_out.equals(0)) {// no time out case
				System.out.println("No Time Out Set");
				isTimeOutRequired = false;			
			}else if(time_out > 0) {
				this.TIME_OUT = time_out;
				System.out.println("Timeout is: " + TIME_OUT);				
			}else {
				log.severe("Incorrect TIME_OUT Value, returning null");
				return null;
			} 		
			
		}else if (time_out == null) {// default case
			this.TIME_OUT = Constants.UDP_TIMEOUT;
			System.out.println("Timeout is default: " + TIME_OUT);
		}

		
		String message = null;
			try {
						
			      ExecutorService service = Executors.newSingleThreadExecutor();
			      CallableMessageFromDatagram task = new CallableMessageFromDatagram(expectedRemoteIP,expectedRemotePort);
			      Future<String> f = service.submit(task);
			      
			      
					try { 
						
						if(isTimeOutRequired) {
							 message =  f.get(TIME_OUT, TimeUnit.MILLISECONDS);
							 service.shutdown();
						}else {
							message = f.get();		      
						    service.shutdown();
						}
					}
					catch (InterruptedException ie) { 
							
						  return null; 
					}
					catch (ExecutionException ee) { 
						  return null;
					}
					catch (TimeoutException te) {
						  log.severe("Receiving Message endeavour has TimedOut:......" );
						  return "time_out";
					}
					catch (Exception e) {
						log.severe("General Exception Occured in UDP Receive TimeOut Thread Terminations case");
						e.printStackTrace();
					}
					if (!service.isTerminated()) { service.shutdownNow();}
			      
			}catch (Exception e) {
				log.severe("UDPReceive receiveBitBoxMessage Method says: "+ e);
				e.printStackTrace();
			}
			//System.out.println("2. UDP Receive is returning: " + message);
			return message;
		
	} 
	
	
			private class CallableMessageFromDatagram implements Callable<String>{
				InetAddress expectedRemoteIP;
				int expectedRemotePort;
				
				public CallableMessageFromDatagram(InetAddress expectedRemoteIP, int expectedRemotePort) {
					this.expectedRemoteIP= expectedRemoteIP;
					this.expectedRemotePort= expectedRemotePort;
				}

				@Override
				public String call() throws Exception {
					String completeMessage = null;		
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DatagramPacket receivedDatagram = null;
					//while(!UDPPortListener.udpPacketsQueue.isEmpty()) {	
					while(true) {
						if(UDPPortProducer.udpPacketsQueue.size()!=0) {
						
							try {
								
								DatagramPacket peekedDatagram = UDPPortProducer.udpPacketsQueue.peek();
								InetAddress receivedPacketIPAddress = peekedDatagram.getAddress();								
								Integer receivedFromPort = peekedDatagram.getPort();								
								boolean fromIntendedSource = receivedPacketIPAddress.equals(expectedRemoteIP) && receivedFromPort == expectedRemotePort;
								
								if(fromIntendedSource) {								
									receivedDatagram = UDPPortProducer.udpPacketsQueue.take();							
									completeMessage = getMessageFromDatagram(receivedDatagram,baos );
									completeMessage.replaceAll("\n", "");
									completeMessage.replaceAll("\r", "");
									System.out.println(this.getClass().getName()+ ": Complete Message at UDP Receive is: " + completeMessage);
									break;
									/*
									if (completeMessage.charAt(completeMessage.length()-1) == '\n') {
										//completeMessage = completeMessage.substring(0, completeMessage.length()-1);
										System.out.println("Complete Message at UDP Receive is: " + completeMessage);
										break;
									}*/
									
								}
							}catch(NullPointerException ne) {
								log.severe("Null Pointer Exception On Receiving Datagram from peer: "
										+ receivedDatagram.getAddress()+":"+receivedDatagram.getPort());
								
								
							}catch (UnsupportedEncodingException ue) {
								log.severe("Unsupported Encoding from On Receiving Datagram from peer: "
										+ receivedDatagram.getAddress()+":"+receivedDatagram.getPort());
								ue.printStackTrace();
							}catch (Exception e){
								log.severe("UDPReceive receiveBitBoxMessage Method says: "+ e);
								e.printStackTrace();
							}
						} 
				}
				
				return completeMessage;
				
			}
				/*This method below
				 * is extracting message from
				 * received datagram. Note that
				 * the bytes returned by getData()
				 * may contain extra unused bytes 
				 * that must be filtered to get actual
				 * message, other JSON won't parse.
				 */
			private String getMessageFromDatagram(DatagramPacket receivedDatagram, ByteArrayOutputStream baos)
					throws UnsupportedEncodingException  {
				String messageInThisBlock = null;
				
					int lengthOfMessage = receivedDatagram.getLength();				
					byte[] dataToReceive  =new byte[lengthOfMessage];
					//System.out.println("Length of blockToReceive before is :" + initialBlockToReceive.length);									
					dataToReceive = receivedDatagram.getData();
					byte[] blockToReceive = new byte[lengthOfMessage];
					
					for(int b=0; b<lengthOfMessage; b++) {
						blockToReceive[b] = dataToReceive[b];
					}
					
					baos.write(blockToReceive, 0, blockToReceive.length);
					messageInThisBlock = baos.toString("UTF-8");
				
						
				return messageInThisBlock;
				
			}
			
	
	} 

}		
