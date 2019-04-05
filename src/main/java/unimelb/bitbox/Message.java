package unimelb.bitbox;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class Message {
	private Command command;
	private String content;
	private String message;
	private byte[] binaryData;
	private String pathName;
	private boolean isSuccessStatus;
	private String md5;
	private String lastModified;

	private int fileSize;
	private int position;
	private int length;
	private List<HostPort> peersList = new ArrayList<>();
	
	public Message(String jsonMessage) {
		//parse jsonMessage
	}
	
	public Message(Document doc) {
		
	}
	
	public String getMessage(Command cmd) {//May be this is a JsonObject
		
		String commandStr = command.name();
		return ""; //Use Document class to construct a message to be transfered
	}
	
	public Command getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = command;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPathName() {
		return pathName;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}

	public boolean isSuccessStatus() {
		return isSuccessStatus;
	}

	public void setSuccessStatus(boolean isSuccessStatus) {
		this.isSuccessStatus = isSuccessStatus;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public List<HostPort> getPeersList() {
		return peersList;
	}

	public void setPeersList(List<HostPort> peersList) {
		this.peersList = peersList;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(byte[] binaryData) {
		content = Base64.encodeBase64String(binaryData);
		this.setBinaryData(binaryData);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}
}
