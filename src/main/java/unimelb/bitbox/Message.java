package unimelb.bitbox;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class Message {
	private Document document;
	private HostPort toAddress;
	private HostPort fromAddress;
	
	private Command command;
	private String content;
	private String message;
	private ByteBuffer binaryData;
	private String pathName;
	private boolean isSuccessStatus;
	private String md5;
	private long lastModified;

	private long fileSize;
	private long position;
	private long length;
	private List<HostPort> peersList = new ArrayList<>();

	private static Logger log = Logger.getLogger(Message.class.getName());
	
	public Message(String jsonMessage) throws Exception {
		//parse jsonMessage
		this(Document.parse(jsonMessage));
	}
	
	//TODO: Should not have this empty constructor
	public Message() {
		
	}
	
	//Copy new instance of an existing message
	public Message(Message msg) {
		this.document = new Document();
		this.toAddress = msg.getToAddress();
		this.fromAddress = msg.getFromAddress();
		
		this.command = msg.getCommand();
		this.content=msg.getContent();
		this.message=msg.getMessage();
		this.binaryData=msg.getBinaryData();
		this.pathName=msg.getPathName();
		this.isSuccessStatus=msg.getSuccessStatus();
		this.md5=msg.getMd5();
		this.lastModified=msg.getLastModified();

		this.fileSize=msg.getFileSize();
		this.position=msg.getPosition();
		this.length=msg.getLastModified();
		this.peersList = msg.getPeersList();
	}
	
	public Message(Document doc) throws Exception {
		//Validate message
		document = doc;
		String cmd = doc.getString("command");
		if (cmd == null || cmd.isEmpty())
				throw new InvalidCommandException("command cannot be empty.");
		
		command = Command.fromString(cmd);
		if (command == null)
			throw new InvalidCommandException("Invalid command.");
		
		parseMessage(command,doc);
	}
	
	private void parseMessage(Command cmd, Document doc) {
		
		//TODO Read and validate tags depending on the incoming command
		switch(cmd) {
		case DIRECTORY_CREATE_REQUEST:
			//TO DO
		case DIRECTORY_CREATE_RESPONSE:
			
		default:
				
		}

		//Read relevant content based on the command
		message = doc.getString("message");
		content = doc.getString("content");
		if((content!=null) && !content.isEmpty()) {
			binaryData = ByteBuffer.wrap(Base64.decodeBase64(content));
		}
		pathName = doc.getString("pathName");
		
		isSuccessStatus = doc.containsKey("isSuccessStatus") ? doc.getBoolean("isSuccessStatus") : false;
		position = doc.containsKey("position") ? doc.getLong("position") : 0;
		length = doc.containsKey("length") ? doc.getLong("length") : 0;
		
		if(doc.containsKey("fileDescriptor"))
		{
			Document fileDescriptor = (Document) doc.get("fileDescriptor");
			md5 = fileDescriptor.getString("md5");
			fileSize = fileDescriptor.getLong("fileSize");
			lastModified = fileDescriptor.getLong("lastModified");
		}
		
		if(doc.containsKey("hostPort")) {
			Document hostDoc = (Document) doc.get("hostPort");
			fromAddress = new HostPort(hostDoc);
		}
		
		if(doc.containsKey("peers")) {
			ArrayList<Document> peerDoc = (ArrayList<Document>) doc.get("peers");
			for (Document pd:peerDoc) {
				peersList.add(new  HostPort(pd));
			}
		}
	}
	
	public String getJsonMessage() {
        return getJsonMessage(command);
	}
	
	public String getJsonMessage(Command cmd) {
		constructDocument(cmd);
		return document.toJson()+"\n";
	}
	
	private void constructDocument(Command cmd) {
        Document doc = new Document();
        Document docFD;
        doc.append("command", cmd.name());
        
        switch (cmd) {
	        case HANDSHAKE_REQUEST:
                Document pd1 = new  Document();
	            pd1.append("host", fromAddress.host);
	            pd1.append("port", fromAddress.port);
	            doc.append("hostPort", pd1);
	        	break;
	        	
	        case INVALID_PROTOCOL:
	        	doc.append("message", message);
	        	break;
	        	
	        case CONNECTION_REFUSED:
	        	doc.append("message", message);
	            ArrayList<Document> peerList = new ArrayList<Document>();
	            for (HostPort p:peersList) {
	                Document pd = new  Document();
		            pd.append("host", p.host);
		            pd.append("port", p.port);
		            peerList.add(pd);
	            }
	            doc.append("peers", peerList);
	            break;

	        case DIRECTORY_CREATE_REQUEST:
	        case DIRECTORY_DELETE_REQUEST:
	            doc.append("pathName", pathName);
	        	break;

	        case DIRECTORY_CREATE_RESPONSE:
	        case DIRECTORY_DELETE_RESPONSE:
	            doc.append("pathName", pathName);
	            doc.append("message", message);
	            doc.append("status", isSuccessStatus);
	        	break;
	            
	        case FILE_BYTES_REQUEST:
	            docFD = new Document();
	            docFD.append("md5",md5);
	            docFD.append("lastModified",lastModified);
	            docFD.append("fileSize",fileSize);
	            
	            doc.append("fileDescriptor", docFD);
	            doc.append("pathName", pathName);
	            doc.append("position", position);
	            doc.append("length", length);
	            
	            break;
	            
	        case FILE_BYTES_RESPONSE:
	            docFD = new Document();
	            docFD.append("md5",md5);
	            docFD.append("lastModified",lastModified);
	            docFD.append("fileSize",fileSize);
	            
	            doc.append("fileDescriptor", docFD);
	            doc.append("pathName", pathName);
	            doc.append("position", position);
	            doc.append("length", length);
	            doc.append("content", content);
	            doc.append("message", message);
	            doc.append("status", isSuccessStatus);
	            break;
	            
	        case FILE_CREATE_RESPONSE:
	        case FILE_DELETE_RESPONSE:
	        case FILE_MODIFY_RESPONSE:
	            docFD = new Document();
	            docFD.append("md5",md5);
	            docFD.append("lastModified",lastModified);
	            docFD.append("fileSize",fileSize);
	            
	            doc.append("fileDescriptor", docFD);
	            doc.append("pathName", pathName);
	            doc.append("message", message);
	            doc.append("status", isSuccessStatus);
	            break;

	        case FILE_CREATE_REQUEST:
	        case FILE_DELETE_REQUEST:
	        case FILE_MODIFY_REQUEST:
	            docFD = new Document();
	            docFD.append("md5",md5);
	            docFD.append("lastModified",lastModified);
	            docFD.append("fileSize",fileSize);
	            
	            doc.append("fileDescriptor", docFD);
	            doc.append("pathName", pathName);
	            break;
	            
		default:
			//throw error
			break;
        }
        //update document object
        this.document = doc;
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

	public boolean getSuccessStatus() {
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

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified2) {
		this.lastModified = lastModified2;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize2) {
		this.fileSize = fileSize2;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
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
	
	public void setContent(ByteBuffer binaryData) {
		content = Base64.encodeBase64String(binaryData.array());
		this.setBinaryData(binaryData);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public ByteBuffer getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(ByteBuffer binaryData) {
		this.binaryData = binaryData;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document doc) throws InvalidCommandException {
		String cmd = doc.getString("command");
		if (cmd == null || cmd.isEmpty())
				throw new InvalidCommandException("command cannot be empty.");
		
		command = Command.fromString(cmd);
		if (command == null)
			throw new InvalidCommandException("Invalid command.");
		
		this.document = doc;
		this.parseMessage(command, document);
	}

	public HostPort getToAddress() {
		return toAddress;
	}

	public void setToAddress(HostPort toAddress) {
		this.toAddress = toAddress;
	}

	public HostPort getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(HostPort fromAddress) {
		this.fromAddress = fromAddress;
	}
}
