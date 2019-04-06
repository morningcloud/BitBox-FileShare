package unimelb.bitbox;

import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Constants.Command;

public class Protocol {
	
	Constants constants = new Constants();
	
	public String createMessage(Constants.Command command, String[] args)
	{
		Document message = null;
		String messageString;
		if (command == Constants.Command.INVALID_PROTOCOL)
		{
			 message = new Document();
			 message.append("command", "INVALID_PROTOCOL");
			 message.append("message", "message must contain a command field as string");
		}
		
		
		messageString = message.toJson();
		
		return messageString;
		
	}

}
