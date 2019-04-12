package unimelb.bitbox;

import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.*;
import java.util.*;
import unimelb.bitbox.util.*;
import unimelb.bitbox.*;
import java.io.File;
import java.nio.file.*;
import java.security.*;
import java.io.*;


public class ProtocolDriver {

	public static void main(String[] args)
	{
		String path = "level/level1/level2";
		for (String s: path.split("/"))
		{
			System.out.println(s);
		}
	}
	public Document TestCreateFile() 
	{
		Document m = new Document();
		try
		{
			File test = new File("g:/My Drive/UGs/cygwin-ug-net.pdf");
			MessageDigest md5Engine = MessageDigest.getInstance("MD5");
			String md5 = getFileChecksum(md5Engine,test);
			long fileSize = test.length();
			long lastModified = System.currentTimeMillis();
			
			
			m.append("command", "FILE_CREATE_REQUEST");
			Document sub = new Document();
			sub.append("fileSize", fileSize);
			sub.append("lastModified", lastModified);
			sub.append("md5", md5);
			m.append("pathName", test.getName());
			m.append("fileDescriptor", sub);
			
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return m;
		
	}

	public Document TestCreateDir() 
	{
		Document m = new Document();
		try
		{
			m.append("command", "DIRECTORY_CREATE_REQUEST");
			m.append("pathName", "level1/level2");
			
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return m;
		
	}
	
	
	private static String getFileChecksum(MessageDigest digest, File file) throws IOException
	{
	    //Get file input stream for reading the file content
	    FileInputStream fis = new FileInputStream(file);
	     
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0;
	      
	    //Read file data and update in message digest
	    while ((bytesCount = fis.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    fis.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	    //return complete hash
	   return sb.toString();
	}
}
