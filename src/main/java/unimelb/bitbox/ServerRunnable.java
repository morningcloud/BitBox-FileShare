package unimelb.bitbox;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ghawady Ehmaid <ghawady.ehmaid@student.unimelb.edu.au>
 * @StudentID: 983899
 * Below just do dummy message passing We should have a list of clients?? or just pick the first item in the config??
 */
public class ServerRunnable implements Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
    
    protected Socket clientSocket = null;
    public ServerRunnable(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(),true);
            
            String data;
            //read data from client
            data = in.readLine();
            log.info("Message from client: " + data);
            
            out.println("Server Acknowledge for message: " + data);
            
        } catch (IOException ex) {
            Logger.getLogger(ServerRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            try {
                if(in != null)
                    in.close();
                if(out != null)
                    out.close();
            } catch (IOException ex) {
                Logger.getLogger(ServerRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
