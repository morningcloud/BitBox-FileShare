package unimelb.bitbox.util;

import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public interface NetworkObserver
{
	public void processNetworkEvent(FileSystemEvent fileSystemEvent);

}
