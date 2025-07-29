import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

class Message {
    private int seqNum;
    private int length;
    private String payload;

    public int getSeqNum() {
        return seqNum;
    }

    public int getLength() {
        return length;
    }

    public String getPayload() {
        return payload;
    }

    public Message(int seqNum, String payload) {
        this.seqNum = seqNum;
        this.payload = payload;
        this.length = payload.length();
    }
}

public class JavaNetClientV2 {

    final static int PORT = 11122;
    final static int CHUNK_SIZE = 1024;
    static DatagramSocket clientSocket;
    static InetAddress SERVER_ADDRESS;
    static Scanner scanner;

    public static void sendWebAddressToServer(String webAddress) throws IOException {
        byte[] webServerURL = new byte[CHUNK_SIZE];
        webServerURL = webAddress.getBytes();
        DatagramPacket webServerPacket = new DatagramPacket(webServerURL, webServerURL.length, SERVER_ADDRESS, PORT);

        // send datagram to server
        clientSocket.send(webServerPacket);
    }

    public static Message receiveResponseFromServer() throws IOException {
        // Receive the response from the server
        // Sequence number + payload length + payload
        byte[] datagramBuffer = new byte[(Integer.BYTES * 2) + CHUNK_SIZE];
        DatagramPacket datagramFromServer = new DatagramPacket(datagramBuffer, datagramBuffer.length);
        clientSocket.receive(datagramFromServer);
        System.out.println(clientSocket.getPort());

        ByteBuffer serverBuffer = ByteBuffer.wrap(datagramFromServer.getData(), 0, datagramFromServer.getLength());

        return new Message(
                // sequence number
                serverBuffer.getInt(),
                // payload
                new String(serverBuffer.array(), serverBuffer.position(), serverBuffer.remaining()));
    }

    public static void main(String args[]) throws Exception {
        clientSocket = new DatagramSocket();
        SERVER_ADDRESS = InetAddress.getLocalHost();
        scanner = new Scanner(System.in);
        System.out.print("Enter a web address: ");
        String webAddress = scanner.nextLine();
        scanner.close();

        sendWebAddressToServer(webAddress);
        boolean fullResReceived = false;
        ByteArrayOutputStream fullRes = new ByteArrayOutputStream();
        while (!fullResReceived) {
            Message msg = receiveResponseFromServer();
            System.out.println(
                    "Received chunk with sequence number: " + msg.getSeqNum() + ", length: " + msg.getLength());
            // Accumulate each chunk of the response
            fullRes.write(msg.getPayload().getBytes());

            // Allocate a buffer for the ACK
            ByteBuffer ackBuffer = ByteBuffer.allocate(Integer.BYTES);
            // Return an ACK datagram with an alternate sequence number
            ackBuffer.putInt(msg.getSeqNum() ^ 1);

            DatagramPacket ackPacket = new DatagramPacket(
                    ackBuffer.array(),
                    Integer.BYTES,
                    SERVER_ADDRESS,
                    PORT);

            // Send ACK back to server
            clientSocket.send(ackPacket);

            if (msg.getLength() < CHUNK_SIZE) {
                fullResReceived = true;
            }
        }
        clientSocket.close();
    }
}
