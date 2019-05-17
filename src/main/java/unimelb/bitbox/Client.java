package unimelb.bitbox;

import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.*;
public class Client
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private static String command="";
	private static HostPort server;
	private static HostPort peer;
	private static Socket socket;
	private static BufferedWriter out; 
	private static BufferedReader in; 
	public static void main(String[] args)
	{
		String[] myArgs = {"-c","list_peers","-s","localhost:8111"};
		//parse(args);
		parse(myArgs);
		authenticate();
		Crypto crypto = new Crypto();
		
		
		//System.out.println(createRequest());
		
		//This class should open a socket and connect it to the received server
		//details from the command line.
	}
	
	private static boolean authenticate()
	{
		//TODO handle java.net.ConnectException
		boolean status = false;
		if (server!=null)
		{
			try
			{
				socket = new Socket(server.host,server.port);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));  
				out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
				
				log.warning("Client Sending Handshake Request to "+server.toString());
				 /*
				  * args[0] --> identity
				 */
				
				//TODO identity needs to be either read from the private key or as a CL argument.
				String identity = "tel@DESKTOP-MG41RRU";
				String[] msgArgs = {identity};
				String authRequest = Protocol.createMessage(Constants.Command.AUTH_REQUEST,msgArgs);
				System.out.println("authRequest="+authRequest);
        		out.write(authRequest);
				out.flush();								 
        					        		
        		log.info("Client Sent Handshake_Request to "+server.toString()+ " message sent: " +authRequest);
        		Document authResponse =  Document.parse(in.readLine());
        		System.out.println("Received response: "+authResponse.toJson());
        		String[] reqArgs = {"",""};
        		String request = Protocol.createMessage(Constants.Command.LIST_PEERS_REQUEST,reqArgs);
        		out.write(request);
				out.flush();	
				Document reply =  Document.parse(in.readLine());
				System.out.println(reply.toJson());
        		
        		//TODO must construct command message and write it to the socket.
				socket.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
		}
		
		return status;
	}
	private static String createRequest()
	{
		String request=""; 
		String reply=""; 
		switch (command)
		{
		case "list_peers":
		{
			String[] reqArgs = {"",""};
			request = Protocol.createMessage(Constants.Command.LIST_PEERS_REQUEST, reqArgs);
			break;
		}
		
		}
		if (socket!=null && !socket.isClosed())
		{
			try
			{
				out.write(request);
				out.flush();
				reply = in.readLine();
				
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				if (socket!=null)
				{
					try
					{
						socket.close();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			
		}
			
		return reply;
	
	}
	private static void parse(String[] args)
	{
		CLArgs clientArgs = new CLArgs();
		
		CmdLineParser parser = new CmdLineParser(clientArgs);
		try
		{
			//Parse the arguments
			parser.parseArgument(args);
			
			//After parsing, the fields in argsBean have been updated with the given
			//command line arguments
			command = clientArgs.getCommand();
			server = new HostPort(clientArgs.getServerHostPort());
			peer = new HostPort(clientArgs.getServerHostPort());
			
		}
		
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			
			//Print the usage to help the user understand the arguments expected
			//by the program
			parser.printUsage(System.err);
		}
		
	}
	
	
	 

}
