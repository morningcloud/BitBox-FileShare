package unimelb.bitbox;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import unimelb.bitbox.util.*;


public class Client
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
	private static String command;
	private static String identity;
	private static HostPort server;
	private static HostPort peer;
	private static Socket socket;
	private static BufferedWriter out; 
	private static BufferedReader in;
	private static Crypto crypto;
	public static void main(String[] args)
	{
		String[] myArgs = {"-c","list_peers","-s","localhost:8111", "-i","tel@DESKTOP-MG41RRU"};
		parse(myArgs);
		connect();
		
		
		
		
		
		//System.out.println(createRequest());
		
		//This class should open a socket and connect it to the received server
		//details from the command line.
	}
	
	public static void processResponse(String response)
	{
		Document responsePayload = Document.parse(response);
		String payload = responsePayload.getString("payload");
		//System.out.println("payload: "+payload);
		String plainResponse = crypto.decrypt(payload);
		//System.out.println("plainResponse: "+plainResponse);
		Document responseDoc = Document.parse(plainResponse);
		String responseCommand = responseDoc.getString("command");
		//System.out.println("responseCommand:"+responseCommand);
		switch (responseCommand)
		{
			case "LIST_PEERS_RESPONSE":
			{
				
				ArrayList<Document> peerList = (ArrayList<Document>) responseDoc.get("peers");
				System.out.println("Output of Connected peers list:");
				System.out.println("-------------------------------");
				for (Document d: peerList) 
				{
					String host = d.getString("host");
					long port = d.getLong("port");
					System.out.println(host+":"+port);
				}
				
				break;
			}
			case "CONNECT_PEER_RESPONSE":
			{
				break;
			}
			case "DISCONNECT_PEER_RESPONSE":
			{
				
				break;
			}
		}
	}
	
	/**
	 * Processes Authentication response received from Peer.
	 * It extracts the session secret key.
	 * @param authResponse
	 * @return
	 */
	public static boolean processAuthResponse(String authResponse)
	{
		boolean status=false;
		Document d = Document.parse(authResponse);
		if (d.getBoolean("status"))
		{
			String encodedKey = d.getString("AES128");
			try
			{
				crypto = new Crypto(encodedKey);
				
				status = (crypto.getSessionKey()==null)?false:true;
			} catch (Exception e)
			{
				
				log.warning(e.getMessage());
			}
		}
		return status;
	}
	
	
	/**
	 * Creates an authentication request JSON string.
	 * @return
	 */
	private static String authenticate()
	{
		
		String[] msgArgs = {identity};
		String authRequest = Protocol.createMessage(Constants.Command.AUTH_REQUEST,msgArgs);
		log.info("Client Sent Handshake_Request to "+server.toString()+ " message sent: " +authRequest);
		return authRequest;
	}
	
	/**
	 * Connects to the designated Peer.
	 * @return
	 */
	private static void connect()
	{
		//TODO handle java.net.ConnectException
		
		if (server!=null)
		{
			try
			{
				socket = new Socket(server.host,server.port);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF8"));  
				out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF8"));
				
				log.info("Client Sending Handshake Request to "+server.toString());
				 /*
				  * args[0] --> identity
				 */
				
				//TODO identity needs to be either read from the private key or as a CL argument.
        		out.write(authenticate());
				out.flush();								 
        					        		
        		String response = in.readLine();
        		
        		if (processAuthResponse(response))
        		{
        			
        			//Processing and sending client request
        			String request = createRequest();
        			Document payloadDoc = new Document();
        			payloadDoc.append("payload", crypto.encrypt(request));
        			out.write(payloadDoc.toJson()+"\n");
        			out.flush();	
        			
        			//Receiving and processing Peer response
        			String replyString = in.readLine();
        			log.info(replyString);
        			processResponse(replyString);
        			//Document replyDoc = Document.parse(replyString);
        			//String payload = replyDoc.getString("payload");
        			//Document reply =  Document.parse(crypto.decrypt(payload));
        			//System.out.println(reply.toJson());
        		}
        		
        		//TODO must construct command message and write it to the socket.
				socket.close();
			}
			catch (ConnectException e)
			{
				log.warning(String.format("Connection to %s failed", server.toString()));
				
			}
			catch (Exception e)
			{
				log.warning(e.getMessage());
			}
			
		}
	}
	
	/**
	 * Creates client requests according to the user's entered command.
	 * @return
	 */
	private static String createRequest()
	{
		String request=""; 
		 
		//reqArgs=null;
		switch (command)
		{
			case "list_peers":
			{
				String[] reqArgs = {"",""};
				request = Protocol.createMessage(Constants.Command.LIST_PEERS_REQUEST, reqArgs);
				break;
			}
			case "connect_peer":
			{
				String[] reqArgs = {peer.host,Integer.toString(peer.port)};
				request = Protocol.createMessage(Constants.Command.CONNECT_PEER_REQUEST, reqArgs);
			
				break;
			}
			case "disconnect_peer":
			{
				String[] reqArgs = {peer.host,Integer.toString(peer.port)};
				request = Protocol.createMessage(Constants.Command.DISCONNECT_PEER_REQUEST, reqArgs);
				break;
			}
		}
		return request;
	}
		
			
		
	
	
	
	/**
	 * Parses CL arguments.
	 * @param args
	 */
	private static void parse(String[] args)
	{
		CLArgs clientArgs = new CLArgs();
		
		CmdLineParser parser = new CmdLineParser(clientArgs);
		try
		{
			//Parse the arguments
			parser.parseArgument(args);
			
			
			command = clientArgs.getCommand();
			server = new HostPort(clientArgs.getServerHostPort());
			peer = new HostPort(clientArgs.getServerHostPort());
			identity = clientArgs.getIdentity();
			
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
