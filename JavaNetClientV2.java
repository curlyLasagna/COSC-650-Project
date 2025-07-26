import java.io.*;
import java.net.*;

public class JavaNetClientV2 {
    public static void main(String args[]) throws Exception {

        //create inpurt stream
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //create client socket
        DatagramSocket clientSocket = new DatagramSocket();
        
        //translate hostname to IP address
        InetAddress IPAddress = InetAddress.getByName("127.0.0.1");

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        System.out.print("Enter a web address: ");
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        //create datagram with data to send
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 11122);

        //send datagram to server
        clientSocket.send(sendPacket);

        //read datagram from server
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        
        String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("FROM SERVER: " + modifiedSentence);

        clientSocket.close();
    }
}
