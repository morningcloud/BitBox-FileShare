package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.logging.Logger;

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
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	public LinkedList<Document> outQueue;

	
	@Override
	public void run()
	{
		/**
		 * This method should continuously monitor InomingEventQueue and call processor to process it.
		 * It is also used to keep track of Synch events which happen every 560 seconds.
		 */
		
	}

	
	/**
	 * Instantiates an EventProcessor.
	 * @param path
	 */
	public EventProcessor(String path)
	{
		log.info("starting event processor");
		if (path!=null)
		{
			try
			{
				this.fileSystemManager = new FileSystemManager(path,this);
				this.outQueue = new LinkedList<Document>();
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public Command processExeternalCommand(Constants.Command command, Document doc)
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
