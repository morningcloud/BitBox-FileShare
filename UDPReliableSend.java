package unimelb.bitbox;
import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import unimelb.bitbox.Connection;
import unimelb.bitbox.util.Configuration;
import java.lang.Math;
import unimelb.bitbox.util.Constants;

public class UDPReliableSend {
		//Header will contain sequence number.
		// sequence number will be of 16 bits
		// 3rd byte will be a flag of last block.
		private static final int HEADER_LENGTH = 3;
		private static final int UDP_ACKNOWLEDGEMENT_WAIT_TIME = Constants.UDP_TIMEOUT;
		private static final int NUMBER_OF_BLOCK_RETRANSMISSIONS = Constants.UDP_NUMBER_OF_RETRIES;
		private static final int FIRST_SEQUENCE_BYTE = 0;
		private static final int SECOND_SEQUENCE_BYTE = 0;
		private static final int FLAG_BYTE_LOCATION = 2;
		private static final int LENGTH_OF_SQN_IN_BYTES = 2;
		
		// To ensure minimum of 8192 Bytes or Block Size
		// is transferred as message to avoid packet sizes
		// greater than maximum packet size. 
		private static int block_size = Math.min(Constants.MAX_BLOCK_SIZE_FOR_UDP,Integer.parseInt(Configuration.getConfigurationValue("blockSize")));

	    public static void sendBitBoxMessage(String IPOfPeer, int portOfPeer, String bitBoxMessage) throws IOException {
	        System.out.println("Message Received for Sending");

	        // Create the socket, set the address and create the file to be sent
	        DatagramSocket socket = new DatagramSocket();
	        InetAddress address = InetAddress.getByName(IPOfPeer);
	        //The message is being converted to byte[] 
	        byte[] messageByteArray = bitBoxMessage.getBytes("UTF-8");
	        int sentBlockSequenceNumber = 0;
	        boolean lastBlockFlag = false;

	        // 16-bit sequence number for acknowledged block sizes
	        int ackSequenceNumber = 0;

	        // Create a counter to count number of retransmissions
	        int retransmissionCounter = 0;

	        // Each Iteration of this for loop process
	        // number of bytes in original message equal to
	        // block size of bitBoxMessage minus Header length.
	        for (int blockNumber=0; blockNumber < messageByteArray.length; blockNumber = blockNumber+ block_size - HEADER_LENGTH ) {

	            // Increment sequence number
	            sentBlockSequenceNumber += 1;

	            // This block from the entire bitBoxMessage
	            // will be sent during this iteration.
	            byte[] blockToSend = new byte[block_size];

	            // Set the first and second bytes of the message to the sequence number
	            blockToSend[FIRST_SEQUENCE_BYTE] = (byte)(sentBlockSequenceNumber >> 8);
	            blockToSend[SECOND_SEQUENCE_BYTE] = (byte)(sentBlockSequenceNumber);

	            // Set flag to 1 if packet is last packet and store it in third byte of header
	            if ((blockNumber+ block_size - HEADER_LENGTH) >= messageByteArray.length) {
	                lastBlockFlag = true;
	                blockToSend[FLAG_BYTE_LOCATION] = (byte)(1);
	            } else { // If not last message store flag as 0
	                lastBlockFlag = false;
	                blockToSend[FLAG_BYTE_LOCATION] = (byte)(0);
	            }

	            // Copy the bytes for the message to the message array
	            if (!lastBlockFlag) {
	                for (int payloadByte=0; payloadByte <= block_size - HEADER_LENGTH - 1; payloadByte++) {
	                    blockToSend[payloadByte+HEADER_LENGTH] = messageByteArray[blockNumber+payloadByte];
	                }
	            }
	            else if (lastBlockFlag) { // Do this if this is the last block in the original message
	                for (int payloadByte=0;  payloadByte < (messageByteArray.length - blockNumber)  ;payloadByte++) {
	                    blockToSend[payloadByte+HEADER_LENGTH] = messageByteArray[blockNumber+payloadByte];			
	                }
	            }

	            // Send the message
	            DatagramPacket block = new DatagramPacket(blockToSend, blockToSend.length, address, portOfPeer);
	            socket.send(block);
	            System.out.println("Sent: Sequence number = " + sentBlockSequenceNumber + ", Last Byte = " + lastBlockFlag);

	            // For verifying the acknowledgements
	            boolean correctAcknowledgementReceived = false;
	            boolean acknoeledgementReceived = false;

	            while (!correctAcknowledgementReceived) {
	                // Check for an ack
	                byte[] ack = new byte[LENGTH_OF_SQN_IN_BYTES];
	                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

	                try {
	                    socket.setSoTimeout(UDP_ACKNOWLEDGEMENT_WAIT_TIME);
	                    socket.receive(ackpack);
	                    ackSequenceNumber = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
	                    acknoeledgementReceived = true;
	                } catch (SocketTimeoutException e) {
	                    System.out.println("Socket timed out waiting for an ack");
	                    acknoeledgementReceived = false;
	                    //e.printStackTrace();
	                }

	                boolean correctAcknowledgement = (acknoeledgementReceived) && (ackSequenceNumber == sentBlockSequenceNumber) ;
	                if (correctAcknowledgement) {	
	                    correctAcknowledgementReceived = true;
	                    System.out.println("Ack received: Sequence Number = " + ackSequenceNumber);
	                    break;
	                } else if (retransmissionCounter <= NUMBER_OF_BLOCK_RETRANSMISSIONS ){ // Resend block for finite number of times
	                    socket.send(block);
	                    System.out.println("Resending: Sequence Number = " + sentBlockSequenceNumber +
	                    		"Retransmission number = " + retransmissionCounter);

	                    // Increment retransmission counter
	                    retransmissionCounter += 1;
	                }
	                else { //Unable to send block within finite block retries
	                	System.out.println("Peer is not reachable");
	                	//TO-DO LOGIC TO REMOVE PEER FROM MEMORIZED LIST
	                	if(socket != null) socket.close();
	                }
	            }
	        }

	        if(socket != null) socket.close();
	        System.out.println("File " + bitBoxMessage + " has been sent");

	    }
	

}
