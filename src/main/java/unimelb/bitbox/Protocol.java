package unimelb.bitbox;

import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Constants.Command;

public class Protocol {
	Peer main;
	public Protocol(Peer main)
	{
		this.main = main;
	}
	
	Constants constants = new Constants();
	
	public String createMessage(Constants.Command command, String[] args)
	{
		Document message = null;
		if (command == Constants.Command.INVALID_PROTOCOL)
		{
			 message = new Document();
			 message.append("message", "message must contain a command field as string");
		}
		if (command == Constants.Command.HANDSHAKE_REQUEST)
		{
			 message = new Document();
			 Document subMessage = new Document();
			 
			 message.append("command", "HANDSHAKE_REQUEST");
			 subMessage.append("host",this.main.serverName);
			 subMessage.append("port", this.main.serverPort);
			 message.append("hostPort", subMessage);
		}
		
		return message.toJson();
	}

}
