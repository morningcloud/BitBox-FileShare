package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

public class Peer 
{
    
	private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Skeleton code - Begin
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();
        
        new ServerMain();
        //Skeleton code - End
        
    	//This adds a handler to the shutdown event... 
        //Possible use, before shutting send a message to clients with this peer's server IP so they reconnect to it instead
    	//May not work with kill command!!
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                log.warning("Application closing!!! Do cleanup");
            }
        }));
        
        log.info("Configuration maximumIncommingConnections= "+Configuration.getConfigurationValue("maximumIncommingConnections"));
                
        /*
        Document doc1 = new Document();
        doc1.append("host","localhost");
        doc1.append("port",8111);
        String host = doc1.getString("host");
        int port = doc1.getInteger("port");
        
        String json1 = doc1.toJson(); // convert Document to a JSON String
        
        Document doc2 = Document.parse(json1); // convert JSON String back to Document
        ArrayList<Document> docs = new ArrayList<Document>();
        docs.add(doc1);
        docs.add(doc2);
        
        Document doc3 = new Document();
        doc3.append("docList",docs);
        doc3.toJson(); // {"docList":[{"host":"localhost","port":8111},{"host":"localhost","port":8111}]}

        log.info("doc1: "+doc1.toJson());
        log.info("doc2: "+doc2.toJson());
        log.info("doc3: "+doc3.toJson());
        log.info("doc3 List: "+doc3.get("docList"));
        ArrayList<Document> docs2 = (ArrayList<Document>) doc3.get("docList");
        */
    }
}
