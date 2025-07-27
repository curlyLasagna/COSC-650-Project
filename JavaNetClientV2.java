import java.io.*;
import java.net.*;

public class JavaNetClientV2 {
    public static void main(String args[]) throws Exception {

        //create input stream
        // TODO: Use Scanner as this is too simple of an input
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //translate hostname to IP address
        InetAddress IPAddress = InetAddress.getByName("localhost");

        // Use a const
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        System.out.print("Enter a web address: ");
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        //create datagram with data to send
        // TODO: Avoid magic number on port. Use a variable name
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 11122);

        //create client socket
        DatagramSocket clientSocket = new DatagramSocket();
        //send datagram to server
        clientSocket.send(sendPacket);

        //read datagram from server
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        clientSocket.receive(receivePacket);
        
        String modifiedSentence =
                new String(
                           // byte []
                           receivePacket.getData(),
                           // offset
                           0,
                           // length
                           receivePacket.getLength());

        System.out.println("FROM SERVER: " + modifiedSentence);

        clientSocket.close();
    }
}
