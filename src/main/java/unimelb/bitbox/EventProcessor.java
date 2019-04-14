package unimelb.bitbox;

import java.io.IOException;
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
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
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
					log.info("External Command Received... Processing "+msg.getDocument().toJson());
					if (msg == null)
						continue;
					if (msg.getCommand() != null) {
						processExternalCommand(msg.getCommand(),msg.getDocument());
					}
				} catch (InterruptedException e) {
					
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
				this.fileSystemManager = new FileSystemManager(path,this);
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
	
	public Command processExternalCommand(Constants.Command command, Document doc)
	{
		Constants.Command response = null;
		
		//Handling file creation requests.
		if (command==Constants.Command.FILE_CREATE_REQUEST)
		{
			/**
			 * Check if file can be created (which FSM APIs do that?)
			 */
			String pathName = doc.getString("pathName");
			Document fileDescriptor = (Document)doc.get("fileDescriptor");
			String md5 = fileDescriptor.getString("md5");
					
			long fileSize = fileDescriptor.getLong("fileSize");
			long lastModified = fileDescriptor.getLong("lastModified");
			
			try
			{
				System.out.println("Actually calling file creator..");
				fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
			}
			
			catch (IOException e)
			{
				log.severe("IO error: "+e.getMessage());
			}
			catch (NoSuchAlgorithmException e)
			{
				log.severe("No such algorithm: "+e.getMessage());
			}
			catch (Exception e)
			{
				log.severe("Unhandled exception: "+e.getMessage());
			}
			
		}
		if (command==Constants.Command.DIRECTORY_CREATE_REQUEST)
		{
			String pathName = doc.getString("pathName");
			if (fileSystemManager.isSafePathName(pathName))
			{
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
			}
		}
		
		
		return response;
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
		
		System.out.printf("Event.toString: %s\n",fileSystemEvent.toString());
		System.out.printf("Event.event: %s\n",fileSystemEvent.event);
		System.out.printf("Event.name: %s\n",fileSystemEvent.name);
		System.out.printf("Event.path: %s\n",fileSystemEvent.path);
		System.out.printf("Event.pathName: %s\n",fileSystemEvent.pathName);
		
		
		//Handling all possible file system manager events.
		
		
		//Handling file creation
		if (fileSystemEvent.event==FileSystemManager.EVENT.FILE_CREATE)
		{
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
			System.out.printf("Event.fileDescriptor:\n"
					+		  "---------------------\n"
					+ "\tlast modified=%s\n"
					+ "\tmd5=%s\n"
					+ "\tsize=%s\n"
					,c.getTime()
					,fileSystemEvent.fileDescriptor.md5
					,fileSystemEvent.fileDescriptor.fileSize);
			
			//TODO: To test full cycle... This may get updated by protocol?
			Message msg = new Message();
			msg.setCommand(Command.FILE_CREATE_REQUEST);
			msg.setMd5(fileSystemEvent.fileDescriptor.md5);
			msg.setFileSize(fileSystemEvent.fileDescriptor.fileSize);
			msg.setLastModified(fileSystemEvent.fileDescriptor.lastModified);
			msg.setPathName(fileSystemEvent.pathName);
			connectionManager.sendAllPeers(msg);
		}

		//Handling file deletion
		if (fileSystemEvent.event==FileSystemManager.EVENT.FILE_DELETE)
		{
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
			System.out.printf("Event.fileDescriptor:\n"
					+		  "---------------------\n"
					+ "\tlast modified=%s\n"
					+ "\tmd5=%s\n"
					+ "\tsize=%s\n"
					,c.getTime()
					,fileSystemEvent.fileDescriptor.md5
					,fileSystemEvent.fileDescriptor.fileSize);
			
		}
		
		//Handling file modification
		if (fileSystemEvent.event==FileSystemManager.EVENT.FILE_MODIFY)
		{
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
			System.out.printf("Event.fileDescriptor:\n"
					+		  "---------------------\n"
					+ "\tlast modified=%s\n"
					+ "\tmd5=%s\n"
					+ "\tsize=%s\n"
					,c.getTime()
					,fileSystemEvent.fileDescriptor.md5
					,fileSystemEvent.fileDescriptor.fileSize);
			
		}
		
		//Handling directory creation
		if (fileSystemEvent.event==FileSystemManager.EVENT.DIRECTORY_CREATE)
		{
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
			System.out.printf("Event.fileDescriptor:\n"
					+		  "---------------------\n"
					+ "\tlast modified=%s\n"
					+ "\tmd5=%s\n"
					+ "\tsize=%s\n"
					,c.getTime()
					,fileSystemEvent.fileDescriptor.md5
					,fileSystemEvent.fileDescriptor.fileSize);
			
		}

		//Handling directory deletion
		if (fileSystemEvent.event==FileSystemManager.EVENT.DIRECTORY_DELETE)
		{
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileSystemEvent.fileDescriptor.lastModified);
			System.out.printf("Event.fileDescriptor:\n"
					+		  "---------------------\n"
					+ "\tlast modified=%s\n"
					+ "\tmd5=%s\n"
					+ "\tsize=%s\n"
					,c.getTime()
					,fileSystemEvent.fileDescriptor.md5
					,fileSystemEvent.fileDescriptor.fileSize);
			
		}
		
		else
		{
			//Should raise an exception that event is unknown.
		}
	}
	
}
