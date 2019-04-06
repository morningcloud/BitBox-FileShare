package unimelb.bitbox;
import java.net.*;
import java.io.*;
import java.util.logging.Logger;

public class Connection extends Thread {
	
	BufferedReader in;
	BufferedWriter out;
	Logger log;
	public Connection(Socket clientSocket) 
	{
		log = Logger.getLogger(Connection.class.getName());
		try
		{
			this.in = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
			
			this.out = new BufferedWriter(
						new OutputStreamWriter(clientSocket.getOutputStream()));
			
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
				
	}
	
	
	@Override
	public void run()
	{
		try
		{
			out.write("Some JSON message");
			
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
		
		
	}

}
