package unimelb.bitbox.util;

import org.kohsuke.args4j.Option;

public class CLArgs
{
	@Option(required = true, name = "-s", aliases = {"--server"}, usage = "Server")
	private String serverHostPort;
	
	@Option(required = false, name = "-p", usage = "Peer")
	private String peerHostPort;

	@Option(required = true, name = "-c", aliases = {"--host"}, usage = "Command")
	private String command;

	

	public String getServerHostPort() {
		return serverHostPort;
	}

	public String getPeerHostPort() {
		return peerHostPort;
	}
	
	public String getCommand() {
		return command;
	}

}


//Remember to add the args4j jar to your project's build path 

//This class is where the arguments read from the command line will be stored
//Declare one field for each argument and use the @Option annotation to link the field
//to the argument name, args4J will parse the arguments and based on the name,  
//it will automatically update the field with the parsed argument value
