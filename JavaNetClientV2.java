import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class JavaNetClientV2 {
    public static void main(String args[]) throws Exception {

        //create inpurt stream
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //create client socket
        DatagramSocket clientSocket = new DatagramSocket();
        
        //translate hostname to IP address
        InetAddress IPAddress = InetAddress.getByName("localhost");

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        System.out.print("Enter a web address: ");
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        //create datagram with data to send
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 11122);

        //send datagram to server
        clientSocket.send(sendPacket);
        
        //Prepare to recieve webpage in chunks 
        boolean done = false;
        ByteArrayOutputStream fullPage = new ByteArrayOutputStream();
        final int chunkSize = 1024;

        while(!done){
            //create buffer for incoming packet
            byte[] receiveDataPacket = new byte[(Integer.BYTES * 2) + chunkSize];
            DatagramPacket receivePacket = new DatagramPacket(receiveDataPacket, receiveDataPacket.length);
            clientSocket.receive(receivePacket);

            //extract data from packet
            ByteBuffer buffer = ByteBuffer.wrap(receivePacket.getData(), 0, receivePacket.getLength());
            int seqNum = buffer.getInt();
            int payloadLength = buffer.getInt();
            
            byte[] payload = new byte[payloadLength];
            buffer.get(payload);
            fullPage.write(payload);

            System.out.println("Received chunk seq=" + seqNum + " size=" + payloadLength);

            //Send ACK back to server
            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            ackBuffer.putInt(seqNum ^ 1);
            DatagramPacket ackPacket = new DatagramPacket(
                ackBuffer.array(),
                ackBuffer.array().length,
                receivePacket.getAddress(),
                receivePacket.getPort() //gets the ephemeral port that the clientHandler thread is using
            );
            clientSocket.send(ackPacket);

            if(payloadLength < chunkSize){
                done = true;
            }
        }
        System.out.println("Received full page: " + fullPage.size() + " bytes");

        clientSocket.close();
    }
}
