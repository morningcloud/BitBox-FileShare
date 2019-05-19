package unimelb.bitbox.util;

public class Constants{

	public static final int BITBOX_SOCKET_TIMEOUT = 3000; //Socket read timeout in milliseconds
	public static final int BITBOX_INCOMING_SOCKET_TIMEOUT = 3000;
	public static final long BITBOX_THREAD_SLEEP_TIME = 10; //Thread Sleep time in milliseconds
	public static final long BITBOX_CONNECTION_THREAD_SLEEP_TIME = 1000; //Thread Sleep time in milliseconds
	public static final long BITBOX_CONNECTION_ATTEMPT_MAX_COUNT = 2; //No of connection attempts to another peer if that connection was not established from first attempt
	public static final String SECRET_KEY_ALGORITHM = "AES";
	public static final int SECRET_KEY_SIZE = 16;
	public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
	public static final String PRIVATE_KEY_FILE_NAME = "bitboxclient_rsa";
	
	public enum State {
	
	}
	
	public enum Command {
		INVALID_PROTOCOL,
		CONNECTION_REFUSED,
		HANDSHAKE_REQUEST,
		HANDSHAKE_RESPONSE,
		FILE_CREATE_REQUEST,
		FILE_CREATE_RESPONSE,
		FILE_DELETE_REQUEST, 
		FILE_DELETE_RESPONSE,
		FILE_MODIFY_REQUEST, 
		FILE_MODIFY_RESPONSE,
		DIRECTORY_CREATE_REQUEST, 
		DIRECTORY_CREATE_RESPONSE,
		DIRECTORY_DELETE_REQUEST, 
		DIRECTORY_DELETE_RESPONSE,
		FILE_BYTES_REQUEST, 
		
		//The following commands/responses are for project2
		
		//1) Commands supplied to Client program:
		list_peers,
		connect_peer,
		disconnect_peer,
		
		//2) Commands sent through Client to Peer.
		AUTH_REQUEST,
		AUTH_RESPONSE,
		LIST_PEERS_REQUEST,
		LIST_PEERS_RESPONSE,
		CONNECT_PEER_REQUEST,
		CONNECT_PEER_RESPONSE,
		DISCONNECT_PEER_REQUEST,
		DISCONNECT_PEER_RESPONSE,
		FILE_BYTES_RESPONSE,
		SYNC_EVENTS;
	    
		public static Command fromString(String cmd)
	    {
	        //look for matching abbreviation and return the equivalent suit
	        for(Command command : Command.values())
	        {
	            if(cmd.equals(command.name()))
	            {
	                return command;
	            }
	        }
	        return null;
	    }
	}
	
	public enum EVENT
	{
		FILE_CREATE_REQUEST,
		FILE_DELETE_REQUEST,
		FILE_BYTES_REQUEST,
		FILE_MODIFY_REQUEST,
		DIRECTORY_CREATE_REQUEST,
		DIRECTORY_DELETE_REQUEST	
	}
	
	public enum PeerSource
	{
		CLIENT,
		SERVER
	}
	
	public enum CryptoUser
	{
		CLIENT,
		PEER
	}
}