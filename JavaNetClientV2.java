import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
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

    public static HashMap<String, Object> receiveResponseFromServer() throws IOException {
        // Receive the response from the server
        // Sequence number + payload length + payload
        byte[] datagramBuffer = new byte[(Integer.BYTES * 2) + CHUNK_SIZE];
        DatagramPacket datagramFromServer = new DatagramPacket(datagramBuffer, datagramBuffer.length);
        clientSocket.receive(datagramFromServer);

        ByteBuffer serverBuffer = ByteBuffer.wrap(datagramFromServer.getData(), 0, datagramFromServer.getLength());

        int serverPort = datagramFromServer.getPort();
        int seqNum = serverBuffer.getInt();
        int payloadLength = serverBuffer.getInt();
        byte[] payloadBytes = new byte[payloadLength];
        serverBuffer.get(payloadBytes);

        String payload = new String(payloadBytes);

        return new HashMap<String, Object>() {
            {
                put("seqNum", seqNum);
                put("port", serverPort);
                put("payload", payload);
            }
        };
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
            HashMap<String, Object> response = receiveResponseFromServer();
            Message msg = new Message(
                    (Integer) response.get("seqNum"),
                    (String) response.get("payload"));

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
                    (Integer) response.get("port"));

            // Send ACK back to server
            clientSocket.send(ackPacket);

            if (msg.getLength() < CHUNK_SIZE) {
                fullResReceived = true;
            }
        }
        clientSocket.close();
    }
}
