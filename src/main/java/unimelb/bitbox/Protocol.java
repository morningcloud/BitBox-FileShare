package unimelb.bitbox;




import unimelb.bitbox.util.Constants;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.HostPort;
import java.util.ArrayList;
import java.util.HashMap;
import unimelb.bitbox.Err.*;
public class Protocol 
{
	
	public static String INVALID_PROTOCOL = "{\"message\":\"message must contain a command field as string\",\"command\":\"INVALID_MESSAGE\"}";
	/*
	 * To make it compatible with client thread and each of it's 
	 * handshake requests, this constructor is not needed.
	 * Moreover Handshake_Request method has also been modified,
	 * where main variable was originally required.
	 * 
	public Protocol(Peer main)
	{
		this.main = main;
	}*/  
	
	/**
	 * Holds all possible events that can occur 
	 *
	 */
	
	
	public static String createRequest(Constants.Command requestType)
	{
		Document requestDocument = new Document();
		
		return requestDocument.toJson();
	}
	public static String createMessage(Constants.Command messageType, String[] args)
	{
		Document response = new Document();
		
		switch (messageType)
		{
	
			/**
			 * Arguments should be structured as:
			 * args[0] -->
			 * args[1] -->
			 * ...
			 */
			case HANDSHAKE_RESPONSE:
			{
				response.append("command", messageType.toString());
				response.append("hostPort", Document.parse(args[0]));
				break;
			}
			
			/**
			 * Arguments should be structured as:
			 * args[0] --> Message to be sent; why the protocol is invalid
			 * args[1] -->
			 * ...
			 */
			case INVALID_PROTOCOL:
			{
				 response.append("command", "INVALID_PROTOCOL");
				 response.append("message", args[0]);
				 break;
			}
			
			case AUTH_REQUEST:
				 response.append("command", "AUTH_REQUEST");
				 response.append("identity", args[0]);
				 break;
				 
			case AUTH_RESPONSE:
				 response.append("command", "AUTH_RESPONSE");
				 response.append("AES128", args[0]);
				 response.append("status", args[1]);
				 response.append("message", args[2]);
				 break;
			
			case HANDSHAKE_REQUEST:
			{	
				 Configuration.getConfiguration();
				 HostPort serverHostPort = new HostPort(Configuration.getConfigurationValue("advertisedName"),
						 Integer.parseInt(Configuration.getConfigurationValue("port")));
				 
				 response.append("command", "HANDSHAKE_REQUEST");								 
				 response.append("hostPort", serverHostPort.toDoc());
				 break;
			}
			default: 
			{
				 response.append("command", "INVALID_PROTOCOL");
				 response.append("message", args[0]);
				 break;
			}
		}
		
		return response.toJson() + "\n"; //appended to include newline character at message response always
	}
	
	public static String validateAuthRequest(Document d)
	{
		String identity = d.getString("identity");
		HashMap<String,ArrayList<String>> authorisedPeers = new HashMap<String,ArrayList<String>>();
		authorisedPeers = Configuration.getAuthKeys();
		String authKey=null;
		if (authorisedPeers.get(identity)!=null)
		{
			authKey = authorisedPeers.get(identity).get(0);
		}
		return authKey;
	}
	/**
	 * Validates a protocol message received according to its type.
	 * @param d
	 * @return
	 */
	public static HostPort validateHS(Document d) throws InvalidCommandException
	{
		HostPort result = null;
		Configuration.getConfiguration();
		
		
		//One validation scenario, more need to be added.
		String command = d.getString("command");
		String msg;
		if (command.equals("HANDSHAKE_REQUEST"))
		{
			Document hostPort = (Document) d.get("hostPort");
			if ((hostPort.getString("host").equals(null)))
			{
				msg = "host is null";
				throw new InvalidCommandException(msg);
			}
			result = new HostPort(hostPort);
		}
		else
		{
			result = null;
		}
		
		return result;
	}
	
	public static boolean validateHSResponse(Document d)
	{
		
	
		try {
			boolean result = true;
			if (d.get("command").equals("HANDSHAKE_RESPONSE")){
			
				Document hostPort = (Document) d.get("hostPort");
				if ((hostPort.getString("host").equals(null))|!((hostPort.getLong("port"))>=1023)){
				
					result = false;					
				}
			}
			else {
			
				result = false;
			}
			return result;
		}
		catch(Exception e){
			return false;			
		}
	}
	public static boolean validateHSRefused(Document d)
	{
		try {				
				boolean result = true;
				if (d.get("command").equals("CONNECTION_REFUSED"))
				{
					System.out.println();
					ArrayList<Document> hostPort = (ArrayList<Document>) d.get("peers");
					//if ((hostPort.getString("host").equals(null))|!((hostPort.getLong("port"))>=1023)){
					
						//result = false;
						
				//	}
				}
				else {
				
					result = false;
				}
			return result;
		}
		catch (Exception e) {
			return false;
		}
	}
}
