package unimelb.bitbox;




import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Document;

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
	
	//not required as all enums are implicitly static
	//Constants constants = new Constants();
	
	
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
			
			case HANDSHAKE_REQUEST:
			{	
				 response.append("command",messageType.toString());
				 String hostAddress = args[0].substring(1);				 
				 Document subMessage = new Document();							 
				 subMessage.append("host", hostAddress); 
				 subMessage.append("port", Long.parseLong(args[1])); 				 
				 response.append("hostPort", subMessage);	
				 response.append("command", "HANDSHAKE_REQUEST");								 
				 System.out.println("Document Generated" + response.toJson());
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
			if ((hostPort.getString("host").equals(null))|!((hostPort.getLong("port"))>=1023))
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
