package unimelb.bitbox;

import java.util.LinkedList;

import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Constants.Command;

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
	 * @author Tarek Elbeik
	 *
	 */
	public enum EVENT
	{
		FILE_CREATE_REQUEST,
		FILE_DELETE_REQUEST,
		FILE_BYTES_REQUEST,
		FILE_MODIFY_REQUEST,
		DIRECTORY_CREATE_REQUEST,
		DIRECTORY_DELETE_REQUEST	
	}
	
	Constants constants = new Constants();
	
	
	
	public <T> String createMessage(Constants.Command command, LinkedList<T> args)
	{
		Document message = null;
		if (command == Constants.Command.INVALID_PROTOCOL)
		{
			 message = new Document();
			 message.append("command", "INVALID_MESSAGE");
			 message.append("message", "message must contain a command field as string");
		}
		if (command == Constants.Command.HANDSHAKE_REQUEST)
		{
			 message = new Document();
			 Document subMessage = new Document();
			 
			 message.append("command", "HANDSHAKE_REQUEST");
			 subMessage.append("host", Peer.serverName); // Modified for static reference
			 subMessage.append("port", Peer.serverPort); // Modified for static reference
			 message.append("hostPort", subMessage);
		}
		if (command == Constants.Command.CONNECTION_REFUSED)
		{
			
		}
		return message.toJson();
	}
	
	/**
	 * Validates a protocol message received according to its type.
	 * @param d
	 * @return
	 */
	public static boolean validate(Document d)
	{
		boolean result = true;
		//One validation scenario, more need to be added.
		if (d.get("command").equals("HANDSHAKE_REQUEST"))
		{
			Document hostPort = (Document) d.get("hostPort");
			if (
					(hostPort.getString("host").equals(null))|!((hostPort.getLong("port"))>=1023))
			{
				result = false;
				
			}
		}
		else
		{
			result = false;
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
				if (d.get("command").equals("HANDSHAKE_REFUSED"))
				{
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
		catch (Exception e) {
			return false;
		}
	}
}
