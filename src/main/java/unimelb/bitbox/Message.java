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

	public String getContent() {
		return content;
	}
	
	public void setcontent(byte[] binaryData) {
		content = Base64.encodeBase64String(binaryData);
		this.setBinaryData(binaryData);
	}

	public void setcontent(String content) {
		content = content;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}
}
