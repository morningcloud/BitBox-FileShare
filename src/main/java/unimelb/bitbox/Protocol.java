package unimelb.bitbox;

import java.util.LinkedList;
import java.util.ArrayList;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Constants.Command;

public class Protocol 
{
	Peer main;
	public static String INVALID_PROTOCOL = "{\"message\":\"message must contain a command field as string\",\"command\":\"INVALID_MESSAGE\"}";
	public Protocol(Peer main)
	{
		this.main = main;
	}
	
	Constants constants = new Constants();
	
	public <T> String createMessage(Constants.Command command, ArrayList<T> args)
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
			 for (T k: args)
			 {
				 //subMessage.append("host", k.);
			 }
			 
			 message.append("command", "HANDSHAKE_REQUEST");
			 subMessage.append("host",this.main.serverName);
			 subMessage.append("port", this.main.serverPort);
			 message.append("hostPort", subMessage);
		}
		if (command == Constants.Command.CONNECTION_REFUSED)
		{
			 message = new Document();
			 Document subMessage = new Document();
			 subMessage.append("peers", args);
			 message.append("command", "HANDSHAKE_REQUEST");
			 subMessage.append("host",this.main.serverName);
			 subMessage.append("port", this.main.serverPort);
			 message.append("hostPort", subMessage);
			
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

}
