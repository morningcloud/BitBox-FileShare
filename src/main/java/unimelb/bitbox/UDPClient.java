package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.Constants.PeerSource;

public class UDPClient {

	private Queue<HostPort> globalBFSQueue;
	public ConnectionManager connectionManager;
	UDPSend udpSend;
	UDPReceive udpReceive;

	public UDPClient(ConnectionManager connectionManager) {
		this.globalBFSQueue = new LinkedList<HostPort>();
		this.connectConfigPeers(Configuration.getConfigurationValue("peers"), this);
		this.connectionManager = connectionManager;
		udpSend = new UDPSend();
		udpReceive = new UDPReceive();
	}

	private void connectConfigPeers(String list, UDPClient peer) {
		String[] hostList = list.split(",");
		for (String h : hostList) {
			PeerRunnable p1 = new PeerRunnable(new HostPort(h), peer);
			p1.start();

		}
	}

	synchronized public Queue<HostPort> getGlobalBFSQueue() {
		return globalBFSQueue;
	}

	private class PeerRunnable extends Thread {

		private HostPort pHostPort;
		private Logger log = Logger.getLogger(PeerRunnable.class.getName());
		// private Protocol protocol;
		private UDPClient peer;


		public PeerRunnable(HostPort pHostPort, UDPClient peer) {
			this.pHostPort = pHostPort;			
			this.peer = peer;

		}
	
		public void run() {
			try {			
			
				Document rxMsg = new Document();
				int attemptsOfHandShakes = 1;
	
				log.warning(this.getName() + ":" + String.format("Trying peer=%s:%s...\n", pHostPort.host, pHostPort.port));
	
				if (UDPPortProducer.serverSocketUDP != null) {
					
					
					
					while (attemptsOfHandShakes <= Constants.UDP_NUMBER_OF_RETRIES &&
							!connectionManager.activePeerHostPort.containsKey(pHostPort.toString())) {
							
							
							// The line below will temporarily stop UDPHandShakeServer to consume
							// packets from UnknownSource pHostPort. This packet will now only
							// be consumed for Handshake consumers.
							if(!connectionManager.temporaryUDPRememberedConnections.containsKey(pHostPort.host.toString())) {								
								connectionManager.temporaryUDPRememberedConnections.put(pHostPort.toString(), pHostPort);
								log.severe("Temporary key has been added for handShake for Peer:...."+pHostPort.toString());
							}else{
								System.out.println("Something is wrong with UDPClient handshake Processing"
										+ ". Breaking the Thread");
								break;
							}
							log.severe("Handshake Attempt: " + attemptsOfHandShakes);
							log.warning(this.getName() + ":"
									+ String.format("Sending Handshake to peer=%s:%s...", pHostPort.host, pHostPort.port));
							String hsr = Protocol.createMessage(Constants.Command.HANDSHAKE_REQUEST, null);
							udpSend.sendBitBoxMessage(pHostPort, hsr);
							
							
							String handshakeResponse;
							
							//Line below start a new consumer, intending to consume packets
							//from remote pHostPort. Second Parameter is Timeout Period.
							//Consumer will wait this amount of time before stopping the
							//consuming thread. If timedout, return message will be null.
							handshakeResponse= udpReceive.receiveBitBoxMessage(pHostPort, null);						
							log.warning(this.getName() + ":"
									+"UDPClient : Received Message-Response from peer " + pHostPort.toString() + " is : "+ handshakeResponse);						
							
														
							
								if (handshakeResponse != null) {
									if (handshakeResponse.equals("time_out")) { //retry to connect within connection attempts.
											attemptsOfHandShakes++;
											log.warning(this.getName() + ":"
													+ String.format("Re-Sending Handshake to peer=%s:%s...attempt is %d", pHostPort.host,
															pHostPort.port, attemptsOfHandShakes));
											try {
												connectionManager.temporaryUDPRememberedConnections.remove(pHostPort.toString());
												//Sends the next HandShake Request after THREAD_SLEP_TIME
												Thread.sleep(Constants.BITBOX_CONNECTION_THREAD_SLEEP_TIME);
											}catch (InterruptedException e) {
												log.warning(this.getName() + ":" + "Thread slept" + e);
											} 
											catch (Exception e) {
												log.severe(this.getClass().getName() + "Exception occured in rxMSg = time_out case of Handshake");
												e.printStackTrace();
											}
										}
										else {//Message is neither null, nor time out, so process it.
												rxMsg = Document.parse(handshakeResponse);
													if (Protocol.validateHSRefused(rxMsg)) {
															log.info("HandShake Refused Validated");
															
															// Since the connection is not maintained, it is 
															// now unknown source again. We must now allow
															// handShakeServer to start consuming packets 
															// back from this source.
															if(connectionManager.temporaryUDPRememberedConnections.containsKey(pHostPort.toString())) {
																connectionManager.temporaryUDPRememberedConnections.remove(pHostPort.toString());
															}else {
																System.out.println("Something is wrong with UDPClient handshake Processing"
																		+ ". Breaking the loop");
																break;
															}
															
															
															ArrayList<Document> peersHostPort = (ArrayList<Document>) rxMsg.get("peers");
															int numOfPeersReceived = 0;
															for (Document hostPort : peersHostPort) {
																System.out.println("Numbers of Peers received" + numOfPeersReceived);
																System.out.println(hostPort.toString());
																peer.getGlobalBFSQueue().add(new HostPort(hostPort));
																numOfPeersReceived++;
															}
															log.warning(this.getName() + String.format("Connection refused from %s. Received %s peer(s).\n",
																	pHostPort.toString(), numOfPeersReceived));
									
															break;
													} else if (Protocol.validateHSResponse(rxMsg)) {//Connection Successful
															Document hostPort = (Document) rxMsg.get("hostPort");
															log.info("Connection established to " + hostPort.getString("host") + " at port "
																	+ hostPort.getLong("port"));
															
															/* This if condition is necessary because 
															 a Consumer thread is started after sending
															 first HandShake Response. However,
															 it is possible that first HandShake Response
															 is lost and the client requests for another
															 handshake. For this second and afterwards
															 request no thread should be started 
															 because one serving them has already
															 been started after first HandShake Response
															 was sent.*/
															if(!connectionManager.activePeerHostPort.containsKey(pHostPort.toString())) {
																connectionManager.rememberUDPConnection(pHostPort);
																new UDPReceiverConsumer(connectionManager, pHostPort).start();
															}
															
													} else {// Nothing is validated, Sending InvalidProtocol
														
														
															// Since the connection was not maintained, it is 
															// now unknown source again. We must now allow
															// handShakeServer to start consuming packets 
															// back from this source.
															if(connectionManager.temporaryUDPRememberedConnections.containsKey(pHostPort.toString())) {
																connectionManager.temporaryUDPRememberedConnections.remove(pHostPort.toString());
															}												
															else {
																System.out.println("Something is wrong with UDPClient handshake Processing"
																		+ ". Breaking the Thread");
																break;
															}							
															
															
															log.warning(String.format("Invalid message received: <%s>.\n", rxMsg));
															String invalidProtocol = Protocol.createMessage(Constants.Command.INVALID_PROTOCOL,
																	"message invalid".split(":"));
															udpSend.sendBitBoxMessage(pHostPort, invalidProtocol);
									
															log.warning(this.getName() + ":" + "Message_Invalid: Peer not remembered");
															// Ask team if a new connection needed after message invalid
															break;
													}
					
									} 
								
								}
						}
						//When the while loop is terminated after successful HandShake
						//it should not do a BFS. That's why below if statement is added. 
						if(!connectionManager.activePeerHostPort.containsKey(pHostPort.toString())) { 
							
							BFSNextPeer();
						}
					
				}else {
					log.severe(this.getName() + ":" + "UDP Port is not Listening. Exiting the Thread");
				}
			
			}catch(NullPointerException ne){
				log.severe("Null Pointer Exeption in UDPCLient "+ this.getName());
			}			
			catch (Exception e){
				log.severe(this.getName() + ":"
						+"General Exception Occured in one of Client's Thread");
				e.printStackTrace();
			}
		}
	
		

		private void BFSNextPeer() {
			log.severe(this.getName() + " : " + "Thread is BFSing next peer");
			if (!peer.getGlobalBFSQueue().isEmpty()) {
				PeerRunnable p1 = new PeerRunnable((HostPort) peer.getGlobalBFSQueue().poll(), this.peer);
				p1.start();
			} else {
				log.severe(this.getName() + " : " + "Thread has failed to find any peer even after BFS to connect");

			}
		}
		

	}
}

/*
 * dChannel = DatagramChannel.open(); log.info(this.getName()+ ":"+
 * " DatagramChannel Opened"); socket = dChannel.socket();
 * log.info(this.getName()+ ":"+ " DatagramChannel bind to socket: " + socket);
 * SocketAddress address = new InetSocketAddress(UDPServer.UDPListenerPort);
 * log.info(this.getName()+ ":"+ " Socket Address Created: " + address);
 * socket.bind(address); log.info(this.getName()+ ":"+
 * " Channel's Socket bind to address " + address); SocketAddress remote = new
 * InetSocketAddress(InetAddress.getByName(pHostPort.host), pHostPort.port);
 * log.info(this.getName()+ ":"+ " Remote Address Created : " + remote);
 * dChannel.connect(remote); log.info(this.getName()+ ":"+
 * " dChannel Connected to Remote : " + remote+ " Is Connected: " +
 * dChannel.isConnected()); socket.setSoTimeout(Constants.UDP_TIMEOUT);
 * log.info(this.getName()+ ":"+ " Socket Receive TimeOujt set to : " +
 * Constants.UDP_TIMEOUT);
 */
