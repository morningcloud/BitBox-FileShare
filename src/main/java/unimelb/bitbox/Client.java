package unimelb.bitbox;

import java.net.Socket;
import java.io.*;
import java.net.UnknownHostException;

public class Client {

    public static void main(String[] args) {
        String host="localhost";
        int port=9831;
        
        try{
            System.out.println("Client started.");
            Socket clientSoc = new Socket(host,port);
            
            //Used to send data to the server
            PrintWriter out = new PrintWriter(clientSoc.getOutputStream(),true);
            
            //Reader to get server reply
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
            
            //Used to read user input
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter a string");
            String str=null;
            
            while(!"exit".equalsIgnoreCase(str)){
                str=userInput.readLine();
                System.out.println("Client says: "+str);
                //write to server
                out.println(str);
                System.out.println(in.readLine());
            }
            
        }
        catch(UnknownHostException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}
