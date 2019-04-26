package unimelb.bitbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.BlockingQueue;


import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Constants;
import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.HostPort;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.util.ArrayList;
import java.util.Calendar;


public class EventProcessor implements FileSystemObserver, Runnable
{
	protected FileSystemManager fileSystemManager;
	private static Logger log = Logger.getLogger(EventProcessor.class.getName());
	public LinkedList<Document> outQueue;
	private BlockingQueue<Message> incomingMessages;
	public ConnectionManager connectionManager;

	/*
	 * This thread constantly takes/monitor incomingMessages from the message queue
	 *  and then call processor to process it.
	 */
	@Override
	public void run()
	{
		/**
		 * This method should continuously monitor incomingMessages and call processor to process it.
		 * It is also used to keep track of Synch events which happen every 560 seconds.
		 */
			while (true) {
				try {
					Message msg = incomingMessages.take(); //This thread will get blocked when the queue is empty and wait till new message get inserted
					log.info(String.format("External Command Received from %s... Processing %s",msg.getFromAddress().toString(),msg.getDocument().toJson()));
					if (msg == null)
						continue;
					if (msg.getCommand() != null) {
						if(msg.getCommand() == Command.SYNC_EVENTS)
							//Generate all file events to the newly connected peer
							processSyncEvents(msg.getFromAddress());
						else
							processExternalCommand(msg.getFromAddress(), msg.getCommand(), msg.getDocument());
					}
					Thread.sleep(Constants.BITBOX_THREAD_SLEEP_TIME);
				} catch (InterruptedException e) {
					//Not required as will occur after sleep
				} catch (RuntimeException rte) {
					rte.printStackTrace();
					//Close?
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
	}

	
	/**
	 * Instantiates an EventProcessor.
	 * @param path
	 */
	public EventProcessor(ConnectionManager connectionManager)
	{
		log.info("starting event processor");
		String path = Configuration.getConfigurationValue("path");
		if (path!=null)
		{
			try
			{
				this.fileSystemManager = new FileSystemManager(path, this);
				this.outQueue = new LinkedList<Document>();
				this.connectionManager = connectionManager;
				this.incomingMessages = connectionManager.getIncomingMessagesQueue();
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else {
			log.log(Level.SEVERE, "Monitor path is undefined. This peer will not initiate any events!");
		}
	}
	
	/**
	 * Handling File System Events. It inspects what event has been raised, marshals/wrap
	 * its data in a Document, and insert it in the OutGoing Queue to be transported by
	 * the TransportAgent.
	 */
	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent)
	{
		/**
		 * We need to filter out duplicate events for manual file/directory creation events
		 * i.e. when creating a new file 3 events fire:
		 * 1) create New file event	--> temporary filename
		 * 2) create fileName event --> the actual file name
		 * 3) delete New file event --> deleting the temporary file.
		 * This does not happen with files copied
		 */
		
		//Handling all possible file system manager events.
		Message msg = constructEventMessage(fileSystemEvent);
		if (msg!=null)
			connectionManager.sendAllPeers(msg);
	}
	

	public void processSyncEvents()
	{
		log.info("Scheduled Interval SyncEvent Generation Started...");
		ArrayList<FileSystemEvent> pathevents=new ArrayList<FileSystemEvent>();
		pathevents = fileSystemManager.generateSyncEvents();
		
		for(FileSystemEvent pathevent : pathevents) {
			log.info(pathevent.toString());
			Message msg = constructEventMessage(pathevent);
			if (msg!=null)
				connectionManager.sendAllPeers(msg);
		}
	}

	public void processSyncEvents(HostPort peer)
	{
		log.info("Peer SyncEvent Generation Started. Peer "+peer.toString());
		ArrayList<FileSystemEvent> pathevents=new ArrayList<FileSystemEvent>();
		pathevents = fileSystemManager.generateSyncEvents();
		
		for(FileSystemEvent pathevent : pathevents) {
			log.info(pathevent.toString());
			//Handling all possible file system manager events.
			Message msg = constructEventMessage(pathevent);
			if (msg!=null)
				connectionManager.sendToPeer(peer, msg, false);
		}
	}
	
	//Put as a function to be reused by SyncEvents
	private Message constructEventMessage(FileSystemEvent fileSystemEvent)
	{

		Calendar c;
		Message msg = null;
		switch(fileSystemEvent.event) {

			case FILE_CREATE:
				//TODO: This may get updated by protocol?
			  msg = new Message();
				msg.setCommand(Command.FILE_CREATE_REQUEST);
				msg.setMd5(fileSystemEvent.fileDescriptor.md5);
				msg.setLastModified(fileSystemEvent.fileDescriptor.lastModified);
				msg.setFileSize(fileSystemEvent.fileDescriptor.fileSize);
				msg.setPathName(fileSystemEvent.pathName);
				System.out.println("TE: pathName="+fileSystemEvent.pathName);
				System.out.println("TE: name="+fileSystemEvent.name);
				System.out.println("TE: path="+fileSystemEvent.path);
				break;
				
			case FILE_DELETE:/*
				c = Calendar.getInstance();
				c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
				System.out.println(String.format("Event.fileDescriptor:\n"
						+		  "---------------------\n"
						+ "\tlast modified=%s\n"
						+ "\tmd5=%s\n"
						+ "\tsize=%s\n"
						,c.getTime()
						,fileSystemEvent.fileDescriptor.md5
						,fileSystemEvent.fileDescriptor.fileSize));
				*/
				msg = new Message();
				msg.setCommand(Command.FILE_DELETE_REQUEST);
				msg.setMd5(fileSystemEvent.fileDescriptor.md5);
				msg.setLastModified(fileSystemEvent.fileDescriptor.lastModified);
				msg.setFileSize(fileSystemEvent.fileDescriptor.fileSize);
				msg.setPathName(fileSystemEvent.pathName);
				
				break;
				
			case FILE_MODIFY:/*
				c = Calendar.getInstance();
				c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
				System.out.println(String.format("Event.fileDescriptor:\n"
						+		  "---------------------\n"
						+ "\tlast modified=%s\n"
						+ "\tmd5=%s\n"
						+ "\tsize=%s\n"
						,c.getTime()
						,fileSystemEvent.fileDescriptor.md5
						,fileSystemEvent.fileDescriptor.fileSize));
				*/
				msg = new Message();
				msg.setCommand(Command.FILE_MODIFY_REQUEST);
				msg.setMd5(fileSystemEvent.fileDescriptor.md5);
				msg.setLastModified(fileSystemEvent.fileDescriptor.lastModified);
				msg.setFileSize(fileSystemEvent.fileDescriptor.fileSize);
				msg.setPathName(fileSystemEvent.pathName);
				
				break;
				
			case DIRECTORY_CREATE:
				msg = new Message();
				msg.setCommand(Command.DIRECTORY_CREATE_REQUEST);
				msg.setPathName(fileSystemEvent.pathName);
				
				break;
				
			case DIRECTORY_DELETE:
				msg = new Message();
				msg.setCommand(Command.DIRECTORY_DELETE_REQUEST);
				msg.setPathName(fileSystemEvent.pathName);
				
				break;
			
			default:
				log.severe(String.format("Unknown File event received %s! Event ignored.", fileSystemEvent.event.name()));
				//Should raise an exception that event is unknown.
				break;
		}
		return msg;
	}
	
	public void processExternalCommand(HostPort senderPeer, Constants.Command command, Document doc)
	{
		//As the response message is a copy of the incoming request, with command and status updated, initialize with message content
		Message responseMessage;
		
		try {
			responseMessage = new Message(doc);
			responseMessage.setFromAddress(senderPeer);
			//GHD: process all known commands
			switch (command) 
			{
				case FILE_CREATE_REQUEST:
					//Handling file creation requests.
					processIncomingFileCreateRequest(responseMessage);
					break;
					
				case DIRECTORY_CREATE_REQUEST:
					processIncomingDirectoryCreateRequest(responseMessage);
					break;
					
				case DIRECTORY_DELETE_REQUEST:
					processIncomingDirectoryDeleteRequest(responseMessage);
					break;

				case FILE_BYTES_REQUEST:
					processIncomingFileByteResquest(responseMessage);
					break;
					
				case FILE_BYTES_RESPONSE:
					processIncomingFileByteResponse(responseMessage);
					break;
					
				case FILE_MODIFY_REQUEST:
					processIncomingFileModifyRequest(responseMessage);
					break;
					
				case FILE_DELETE_REQUEST:
					processIncomingFileDeleteRequest(responseMessage);
					break;
					
				case DIRECTORY_CREATE_RESPONSE:
				case DIRECTORY_DELETE_RESPONSE:
				case FILE_CREATE_RESPONSE:
				case FILE_DELETE_RESPONSE:
				case FILE_MODIFY_RESPONSE:
					//No action to be done, just log drop the message
					log.info(String.format("%s received but no action needed... Dropped.", command.name()));
					break;
				
				case HANDSHAKE_REQUEST:
					//At this stage the handshake would have been established and validated already... 
					//so this request should be rejected
					connectionManager.sendToPeer(senderPeer, Protocol.createMessage(Command.INVALID_PROTOCOL, "was not expecting a HANDSHAKE_REQUEST".split(";")), true);
					break;
				case INVALID_PROTOCOL:
					//If received here, something is wrong with one of our messages or peer have issue processing a proper message!
					//TODO: what to do here?
					//I think it is OK if we do nothing, as ideally they should disconnect from us once they
					log.severe(String.format("Unexpected INVALID_PROTOCOL received from %s!", senderPeer));
					break;
					
				default:
					//construct invalid protocol message
					connectionManager.sendToPeer(senderPeer, Protocol.createMessage(Command.INVALID_PROTOCOL, "unknown command".split(";")), true);
					break;
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void processIncomingDirectoryCreateRequest(Message message) {
		String pathName = message.getPathName();
		message.setCommand(Command.DIRECTORY_CREATE_RESPONSE);
		
		if (fileSystemManager.isSafePathName(pathName))
		{
			if (fileSystemManager.dirNameExists(pathName))
			{
				message.setSuccessStatus(false);
				message.setMessage("pathname already exists");
			}
			else {
				try {
					if (fileSystemManager.makeDirectory(pathName))
					{
						message.setSuccessStatus(true);
						message.setMessage("directory created.");
					}
					else {
						log.severe("fileSystemManager.makeDirectory returned false!!");
						message.setSuccessStatus(false);
						message.setMessage("there was a problem creating the directory");
					}
				}catch(Exception e) {
					log.severe("Exception in makeDirectory, rejecting request.");
					e.printStackTrace();
					message.setSuccessStatus(false);
					message.setMessage("there was a problem creating the directory");
				}
			}
		}else {
			message.setSuccessStatus(false);
			message.setMessage("unsafe path name given");
		}
		
		sendResponse(message);
			
			//GHD: commented below as I don't think it is needed
			/*
			LinkedList<String> dirs = new LinkedList<String>();
			//populate the Queue with directories separated by "/"
			for (String dir: pathName.split("/"))
			{
				dirs.add(dir);
			}
			try
			{
				boolean isCreated = true;
				String dir="";
				
				//Creating directories starting with leaves, stops if creating any sub-folder fails.
				while (!dirs.isEmpty()&isCreated)
				{
					dir += "/"+ dirs.pop();
					isCreated = fileSystemManager.makeDirectory(dir);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Invalid path!");
		}*/
	}
	
	private void processIncomingDirectoryDeleteRequest(Message message) {
		String pathName = message.getPathName();
		message.setCommand(Command.DIRECTORY_DELETE_RESPONSE);
		if (fileSystemManager.isSafePathName(pathName))
		{
			if (!fileSystemManager.dirNameExists(pathName))
			{
				message.setSuccessStatus(false);
				message.setMessage("pathname does not exist");
			}
			else {
				try {
					if (fileSystemManager.deleteDirectory(pathName))
					{
						message.setSuccessStatus(true);
						message.setMessage("directory deleted");
					}
					else {
						log.severe("fileSystemManager.deleteDirectory returned false!!");
						message.setSuccessStatus(false);
						message.setMessage("there was a problem deleting the directory");
					}
				}catch(Exception e) {
					log.severe("Exception in makeDirectory, rejecting request.");
					e.printStackTrace();
					message.setSuccessStatus(false);
					message.setMessage("there was a problem deleting the directory");
				}
			}
		}
		else {
			message.setSuccessStatus(false);
			message.setMessage("unsafe path name given");
		}

		sendResponse(message);
	}
	
	private void processIncomingFileCreateRequest(Message message) {
		/**
		 * Check if file can be created (which FSM APIs do that?)
		 */
		String pathName = message.getPathName();
		String md5 = message.getMd5();
		long fileSize = message.getFileSize();
		long lastModified = message.getLastModified();
		long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
		//In case the file is small, set the block size to that for complete file fetching
		if (fileSize < blockSize)
			blockSize = fileSize;
		
		message.setCommand(Command.FILE_CREATE_RESPONSE);
		try
		{	
			if (fileSystemManager.isSafePathName(pathName))
			{
				if (fileSystemManager.fileNameExists(pathName))
				{
					message.setSuccessStatus(false);
					message.setMessage("pathname already exists");
				}
				else {
					try {
						if (fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified))
						{
							message.setSuccessStatus(true);
							message.setMessage("file loader ready");
							//if checkShortcut returns false it would have copied a local folder already and no further action needed.
							// otherwise we need to initiate a file_byte_request
							if(!fileSystemManager.checkShortcut(pathName)) {
								//We need to initiate the first file byte request message
								Message byteReqMessage = new Message(message);
								byteReqMessage.setCommand(Command.FILE_BYTES_REQUEST);
								byteReqMessage.setPosition(0); //since it is first request the position is 0
								byteReqMessage.setLength(blockSize);
								sendResponse(byteReqMessage);
							}
							else {
								log.info(String.format("Another local file already exists with the same content of %s... Shortcut copy done.",pathName));
							}
						}
						else {
							log.severe("fileSystemManager.createFileLoader returned false!!");
							message.setSuccessStatus(false);
							message.setMessage("there was a problem creating the file");
						}
					}
					catch (IOException e)
					{
						log.severe("IO error: "+e.getMessage());
						e.printStackTrace();
						message.setSuccessStatus(false);
						message.setMessage("there was a problem creating the file");
					}
					catch (NoSuchAlgorithmException e)
					{
						log.severe("No such algorithm: "+e.getMessage());
						e.printStackTrace();
						message.setSuccessStatus(false);
						message.setMessage("there was a problem creating the file");
					}
					catch(Exception e) {
						log.severe("Exception in file create, rejecting request.");
						e.printStackTrace();
						message.setSuccessStatus(false);
						message.setMessage("there was a problem creating the file");
					}
				}
			}else {
				message.setSuccessStatus(false);
				message.setMessage("unsafe path name given");
			}
			
			sendResponse(message);
				
		}
		catch (Exception e)
		{
			log.severe("Unhandled exception: "+e.getMessage());
			e.printStackTrace();
		}
	}

	private void processIncomingFileDeleteRequest(Message message) {
		String pathName = message.getPathName();
		String md5 = message.getMd5();
		long lastModified = message.getLastModified();

		message.setCommand(Command.FILE_DELETE_RESPONSE);
		try
		{	
			if (fileSystemManager.isSafePathName(pathName))
			{
				//We should verify that the exact file with the same content exists for deletion
				// to avoid deleting same file name with different content
				if (!fileSystemManager.fileNameExists(pathName))
				{
					message.setSuccessStatus(false);
					message.setMessage("pathname does not exist");
				}else if (!fileSystemManager.fileNameExists(pathName, md5))
				{
					message.setSuccessStatus(false);
					message.setMessage("there was a problem deleting the file, file exists but with different content");
				}else {
					try {
						if (fileSystemManager.deleteFile(pathName, lastModified, md5))
						{
							message.setSuccessStatus(true);
							message.setMessage("file deleted");
						}
						else {
							log.severe("fileSystemManager.deleteFile returned false!!");
							message.setSuccessStatus(false);
							message.setMessage("there was a problem deleting the file");
						}
					}
					catch(Exception e) {
						log.severe("Exception in file delete, rejecting request.");
						e.printStackTrace();
						message.setSuccessStatus(false);
						message.setMessage("there was a problem deleting the file");
					}
				}
			}else {
				message.setSuccessStatus(false);
				message.setMessage("unsafe path name given");
			}
			
			sendResponse(message);
				
		}
		catch (Exception e)
		{
			log.severe("Unhandled exception: "+e.getMessage());
		}
	}
	
	private void processIncomingFileModifyRequest(Message message) {
		String pathName = message.getPathName();
		String md5 = message.getMd5();
		long fileSize = message.getFileSize();
		long lastModified = message.getLastModified();
		long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
		//In case the file is small, set the block size to that for complete file fetching
		if (fileSize < blockSize)
			blockSize = fileSize;

		message.setCommand(Command.FILE_MODIFY_RESPONSE);
		try
		{	
			if (fileSystemManager.isSafePathName(pathName))
			{
				if (!fileSystemManager.fileNameExists(pathName))
				{
					message.setSuccessStatus(false);
					message.setMessage("pathname does not exist");
				}
				else if (fileSystemManager.fileNameExists(pathName, md5))
				{
					message.setSuccessStatus(false);
					message.setMessage("file already exists with matching contents");
				}
				else {
					try {
						if (fileSystemManager.modifyFileLoader(pathName, md5, lastModified))
						{
							message.setSuccessStatus(true);
							message.setMessage("file loader ready");
							//if checkShortcut returns false it would have copied a local folder already and no further action needed.
							// otherwise we need to initiate a file_byte_request
							//TODO is checkShortcut needed in modify case???
							if(!fileSystemManager.checkShortcut(pathName)) {
								//We need to initiate the first file byte request message
								Message byteReqMessage = new Message(message);
								byteReqMessage.setCommand(Command.FILE_BYTES_REQUEST);
								byteReqMessage.setPosition(0); //since it is first request the position is 0
								byteReqMessage.setLength(blockSize);
								sendResponse(byteReqMessage);
							}
						}
						else {
							log.severe("fileSystemManager.modifyFileLoader returned false!!");
							message.setSuccessStatus(false);
							message.setMessage("there was a problem modifying the file");
						}
					}
					catch (IOException e)
					{
						log.severe("IO error: "+e.getMessage());
						message.setSuccessStatus(false);
						message.setMessage("there was a problem modifying the file");
						e.printStackTrace();
					}
					catch (NoSuchAlgorithmException e)
					{
						log.severe("No such algorithm: "+e.getMessage());
						message.setSuccessStatus(false);
						message.setMessage("there was a problem modifying the file");
						e.printStackTrace();
					}
					catch(Exception e) {
						log.severe("Exception in file modify, rejecting request.");
						e.printStackTrace();
						message.setSuccessStatus(false);
						message.setMessage("there was a problem creating the file");
					}
				}
			}else {
				message.setSuccessStatus(false);
				message.setMessage("unsafe path name given");
			}
			
			sendResponse(message);
				
		}
		catch (Exception e)
		{
			log.severe("Unhandled exception: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void processIncomingFileByteResponse(Message message) {
		String pathName = message.getPathName();
		long position = message.getPosition();
		ByteBuffer writeBuffer = message.getBinaryData();
		long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
		boolean send = false;
		boolean cancelfile = false;
		long fileSize = message.getFileSize();
		//In case the file is small, set the block size to that for complete file fetching
		//if (fileSize < blockSize)
		//	blockSize = fileSize;
		try
		{	
			//TODO: any need to re-send the same File byte request message again in case of failure??
			//TODO: what to do in case the byte write fail? currently just closing the file loader
			message.setCommand(Command.FILE_BYTES_RESPONSE);
			if (fileSystemManager.isSafePathName(pathName))
			{
				try {
					if (fileSystemManager.writeFile(pathName, writeBuffer, position))
					{
						if(fileSystemManager.checkWriteComplete(pathName)) {
							//If Write Complete do nothing
							send = false;
						}
						else {
							//We need to initiate the first file byte request message
							Message byteReqMessage = new Message(message);
							byteReqMessage.setCommand(Command.FILE_BYTES_REQUEST);
							position += blockSize;
							byteReqMessage.setPosition(position); //request next position
							//byteReqMessage.setFileSize(position + blockSize); 
							//get how much left to read
							long toread = fileSize - position;
							//if what is left less than the block size, set the length to that
							if (toread <= 0) {
								log.severe("Unexpected behaviour... checkWriteComplete returned false, but no more bytes left to read!");
							}
							else {
								if (toread < blockSize)
									blockSize = toread;
								byteReqMessage.setLength(blockSize);
								sendResponse(byteReqMessage);
							}
						}
					}
					else {
						log.severe("fileSystemManager.writeFile returned false!!");
						message.setSuccessStatus(false);
						message.setMessage("unsuccessful write - no such file with that content");
						cancelfile=true;
					}
				}
				catch (IOException e)
				{
					log.severe("IO error: "+e.getMessage());
					message.setSuccessStatus(false);
					message.setMessage("there was a problem writing the file");
					cancelfile=true;
					e.printStackTrace();
				}
				catch (NoSuchAlgorithmException e)
				{
					log.severe("No such algorithm: "+e.getMessage());
					message.setSuccessStatus(false);
					message.setMessage("there was a problem writing the file");
					cancelfile=true;
					e.printStackTrace();
				}
				catch(Exception e) {
					log.severe("Exception in file write, rejecting request.");
					e.printStackTrace();
					message.setSuccessStatus(false);
					message.setMessage("there was a problem write the file");
					cancelfile=true;
					e.printStackTrace();
				}
			}else {
				message.setSuccessStatus(false);
				message.setMessage("unsafe path name given");
				cancelfile=true;
			}
			
			if (cancelfile)
				fileSystemManager.cancelFileLoader(pathName);
			
			if (send)
				sendResponse(message);
				
		}
		catch (Exception e)
		{
			log.severe("Unhandled exception: "+e.getMessage());
		}
	}
	
	private void processIncomingFileByteResquest(Message message) {
		String pathName = message.getPathName();
		String md5 = message.getMd5();
		long position = message.getPosition();
		long length = message.getLength();
		ByteBuffer readBuffer;
		try
		{	
			message.setCommand(Command.FILE_BYTES_RESPONSE);
			if (fileSystemManager.isSafePathName(pathName))
			{
				try {
					readBuffer = fileSystemManager.readFile(md5, position, length);
					if (readBuffer != null)
					{
						message.setSuccessStatus(true);
						message.setMessage("successful read");
						message.setContent(readBuffer);
					}
					else {
						message.setSuccessStatus(false);
						message.setMessage("unsuccessful read - no such file with that content");
					}
				}
				catch (IOException e)
				{
					log.severe("IO error: "+e.getMessage());
					message.setSuccessStatus(false);
					message.setMessage("there was a problem reading the file");
					e.printStackTrace();
				}
				catch (NoSuchAlgorithmException e)
				{
					log.severe("No such algorithm: "+e.getMessage());
					message.setSuccessStatus(false);
					message.setMessage("there was a problem reading the file");
					e.printStackTrace();
				}
				catch(Exception e) {
					log.severe("Exception in file read, rejecting request.");
					e.printStackTrace();
					message.setSuccessStatus(false);
					message.setMessage("there was a problem reading the file");
				}
			}else {
				message.setSuccessStatus(false);
				message.setMessage("unsafe path name given");
			}
			
			sendResponse(message);
				
		}
		catch (Exception e)
		{
			log.severe("Unhandled exception: "+e.getMessage());
		}
	}
	
	private void sendResponse(Message responseMessage) {
		//GHD: After processing send the response message back to peer
		try {
			connectionManager.sendToPeer(responseMessage.getFromAddress(), responseMessage, false);
		}
		catch (Exception e) {
			// TODO: handle exception
			log.severe("Error sending event process response!");
			e.printStackTrace();
		}
	}

}
