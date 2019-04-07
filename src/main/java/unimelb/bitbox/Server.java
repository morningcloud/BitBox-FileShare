package unimelb.bitbox;

import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.FileSystemObserver;

public class Server implements FileSystemObserver {
	
	public Server()
	{
		
		
	}
	
	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// TODO: process events Check event type and process

		System.out.println("[TE-INSIDE]: "+String.format("Event Raised. EventType: %s FileName: '%s' Path: '%s'", 
				fileSystemEvent.event.toString(), fileSystemEvent.name, fileSystemEvent.path));
	}

}
