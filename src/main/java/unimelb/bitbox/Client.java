package unimelb.bitbox;

import java.util.logging.Logger;

import unimelb.bitbox.util.Constants;

public class Client
{
	private static Logger log = Logger.getLogger(Peer.class.getName());
	public static void main(String[] args)
	{
		String command="";
		String server="";
		String peer="";
		
		//This class should open a socket and connect it to the received server
		//details from the command line.
		
		
		//The minimum number of arguments expected is 4
		if (args.length>=4)
		{
			//Checking if first argument is the command flag
			if (args[0].equals("-c"))
			{
				//Checking that the command entered is legal
				//TODO Need to flesh out the methods below.
				command = args[1];
				if (command.equals(Constants.Command.list_peers.toString()))
				{
					System.out.println("suplied command is list pleers.");
				}
				else if (command.equals(Constants.Command.connect_peer.toString()))
				{
					System.out.println("suplied command is connect peer.");
				}
				else if (command.equals(Constants.Command.disconnect_peer.toString()))
				{
					System.out.println("suplied command is disconnect peer.");
				}
				else
				{
					log.severe(String.format("unknown command %s. valid commands are (list_peers,connect_peer,disconnect_peer). Exiting Client program\n",args[1]));
					System.exit(10);
				}
				
			}
			else
			{
				log.severe(String.format("unknown argument %s. Expective -c <command>. Exiting Client program\n",args[0]));
				System.exit(10);
			}
		}
		

	}
	
	 

}
