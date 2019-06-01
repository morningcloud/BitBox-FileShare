package unimelb.bitbox;

import unimelb.bitbox.util.Constants;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import unimelb.bitbox.Err.*;

public class Protocol {
	private static Logger log = Logger.getLogger(Protocol.class.getName());
	public static String INVALID_PROTOCOL = "{\"message\":\"message must contain a command field as string\",\"command\":\"INVALID_MESSAGE\"}";
	/*
	 * To make it compatible with client thread and each of it's handshake requests,
	 * this constructor is not needed. Moreover Handshake_Request method has also
	 * been modified, where main variable was originally required.
	 * 
	 * public Protocol(Peer main) { this.main = main; }
	 */

	/**
	 * Holds all possible events that can occur
	 *
	 */

	public static String createRequest(Constants.Command requestType) {
		Document requestDocument = new Document();

		return requestDocument.toJson();
	}

	public static String createMessage(Constants.Command messageType, String[] args) {
		Document response = new Document();

		switch (messageType) {

		/**
		 * list_peers args[0] --> N/A args[1] --> N/A
		 */

		case list_peers: {
			response.append("command", Constants.Command.LIST_PEERS_REQUEST.toString());
			break;

		}

		case connect_peer: {
			/**
			 * connect_peer args[0] --> host args[1] --> port
			 */

			response.append("command", Constants.Command.CONNECT_PEER_REQUEST.toString());
			response.append("host", args[0]);
			response.append("port", Integer.parseInt(args[1]));

			break;

		}
		case disconnect_peer: {
			/**
			 * disconnect_peer args[0] --> host args[1] --> port
			 */

			response.append("command", Constants.Command.DISCONNECT_PEER_REQUEST.toString());
			response.append("host", args[0]);
			response.append("port", Integer.parseInt(args[1]));

			break;

		}

		/**
		 * Arguments should be structured as: args[0] --> args[1] --> ...
		 */
		case HANDSHAKE_RESPONSE: {
			response.append("command", messageType.toString());
			response.append("hostPort", Document.parse(args[0]));
			break;
		}

		/**
		 * Arguments should be structured as: args[0] --> Message to be sent; why the
		 * protocol is invalid args[1] --> ...
		 */
		case INVALID_PROTOCOL: {
			response.append("command", "INVALID_PROTOCOL");
			response.append("message", args[0]);
			break;
		}

		case AUTH_REQUEST: {
			/*
			 * args[0] --> identity
			 */
			response.append("command", "AUTH_REQUEST");
			response.append("identity", args[0]);
			break;
		}

		case LIST_PEERS_REQUEST: {
			/*
			 * args[0] --> identity
			 */
			response.append("command", "LIST_PEERS_REQUEST");
			break;
		}

		case AUTH_RESPONSE: {
			/*
			 * authentication response. args[0] --> AES128 Base64 encoded, encrypted secret
			 * key using the requestor's public key args[1] --> true/false
			 */
			boolean status = (args[1].equals("true")) ? true : false;
			response.append("command", "AUTH_RESPONSE");
			if (args[0] != null)
				response.append("AES128", args[0]);
			response.append("status", status);
			response.append("message", (status == true) ? "public key found" : "public key not found");

			break;
		}
		case HANDSHAKE_REQUEST: {

			response.append("command", "HANDSHAKE_REQUEST");
			response.append("hostPort", Peer.serverHostPort.toDoc());
			break;
		}
		default: {
			response.append("command", "INVALID_PROTOCOL");
			response.append("message", args[0]);
			break;
		}
		}

		return response.toJson() + "\n"; // appended to include newline character at message response always
	}

	/*
	 * 
	 */
	public static String createUDPHandShakeResponse(String inmsg, ConnectionManager connectionManager) {
		try {

			Document handshakeMsg = Document.parse(inmsg);
			HostPort connectingPeer = Protocol.validateHS(handshakeMsg);
			//System.out.println("Protocol Validation Said :" + connectingPeer.toString());

			if (connectingPeer != null) {
				// If HANDSHAKE_REQUEST is valid but the server is at it's maximum incoming
				// connection capacity
				// a CONNECTION_REFUSED message is sent and the connection is closed.
				if (connectionManager.getUDPRememberedConnectionCount() >= connectionManager.MAX_NO_OF_CONNECTION) {
					Document response = new Document();
					response.append("command", "CONNECTION_REFUSED");
					response.append("message", "connection limit reached");
					response.append("peers", connectionManager.getPeerList());
					log.info(String.format("Connection from %s refused.\n Message sent: %s\n",
							connectingPeer.toString(), response.toJson()));
					return response.toJson() + "\n";
				}

				else {// if the HANDSHAKE_REQUEST is valid and the server has capacity, the connection
						// is added to ConnectionManager, and a HANDSHAKE_RESPONSE message is sent to the
						// connecting peer.
					
					Document response = new Document();
					response.append("command", "HANDSHAKE_RESPONSE");
					response.append("hostPort", new HostPort(Peer.serverName, Peer.udpServerPort).toDoc());
					//System.out.println("2. Sending HandShake Response");
					log.info(String.format("Connected to: %s, total number of established connections: %s\n",
							connectingPeer, connectionManager.getUDPRememberedConnectionCount()));
					
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
					if(!connectionManager.activePeerHostPort.containsKey(connectingPeer.toString())) {
						connectionManager.rememberUDPConnection(connectingPeer);
						new UDPReceiverConsumer(connectionManager, connectingPeer).start();
						log.info("HandShakeServer has added Host " + connectingPeer.toString() + " to UDP remembered list after sending Handshake_Response");
					}
					log.info(String.format("Connected to: %s, total number of established connections: %s\n",
							connectingPeer, connectionManager.getUDPRememberedConnectionCount()));
					return response.toJson() + "\n";

				}
			} else {
				// if something other than handshake_request
				// is received from unknown hosts, decline it.
				log.info(String.format("Invalid Protocol being sent to %s. ", connectingPeer.toString()));
				return Protocol.invalidProtocol();
			}

		} catch (InvalidCommandException e) {
			// If HANDSHAKE_REQUEST is invalid, respond
			// with INVALID_PROTOCOL and close connection.
			log.info("Invalid Command Exception ocured in createUDPHandShakeResponse, sending InvalipP" + e);

		} catch (Exception e2) {
			log.severe("General exception while processing method createUDPHandShakeResponse");
			e2.printStackTrace();
		}

		return Protocol.invalidProtocol(); // If neither HSRefued not HSResponse, return InvalidP
	}

	public static String invalidProtocol() {
		Document response = new Document();
		response.append("command", "INVALID_PROTOCOL");
		response.append("message", "invalid message, expecting HANDSHAKE_REQUEST");

		return response.toJson() + "\n";

	}

	/**
	 * Validates a protocol message received according to its type.
	 * 
	 * @param d
	 * @return
	 */
	public static HostPort validateHS(Document d) throws InvalidCommandException {
		HostPort result = null;
		Configuration.getConfiguration();
		String command = d.getString("command");
		String msg;
		if (command.equals("HANDSHAKE_REQUEST")) {
			Document hostPort = (Document) d.get("hostPort");
			if ((hostPort.getString("host").equals(null))) {
				msg = "host is null";
				throw new InvalidCommandException(msg);
			}
			result = new HostPort(hostPort);
		} else {
			result = null;
		}

		return result;
	}

	public static boolean validateHSResponse(Document d) {
		log.info("Protocol Trying to Validate HandShake Respinse");

		try {
			boolean result = true;
			if (d.getString("command").equals("HANDSHAKE_RESPONSE")) {

				Document hostPort = (Document) d.get("hostPort");
				if ((hostPort.getString("host").equals(null)) | !((hostPort.getLong("port")) >= 1023)) {

					result = false;
				}
			} else {

				result = false;
				log.info("Protocol failed to validate HandShake Response");
			}
			return result;
		} catch (Exception e) {
			log.severe("Protocol Class: Exception thrown while trying to  Validate Handshake_Response: " + e);
			e.printStackTrace();
			return false;
		}
	}

	public static boolean validateHSRefused(Document d) {
		try {
			log.info("Protocol Trying to Validate HandShake Refused");
			boolean result = true;

			String command = d.getString("command");
			if (command.equals("CONNECTION_REFUSED")) {
				System.out.println();
				ArrayList<Document> hostPort = (ArrayList<Document>) d.get("peers");

			} else {
				result = false;
				log.info("Protocol Failed to Validate HandShake Refused");
			}
			return result;
		} catch (Exception e) {
			log.severe("Protocol Class: Exception thrown while trying to  Validate Handshake_Refused: " + e);
			e.printStackTrace();
			return false;
		}
	}
}
