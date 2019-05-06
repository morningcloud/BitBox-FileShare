package unimelb.bitbox.util;

import unimelb.bitbox.Connection;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public interface NetworkObserver
{
	public void processNetworkEvent(FileSystemEvent fileSystemEvent);
	public void connectionClosed(HostPort connectionID);
	public void messageReceived(HostPort connectionID, String message);
}
