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

    /**
     * Initial client datagram to send the web address to the server.
     * 
     * @param webAddress: String The web address to send to the server.
     * @throws IOException
     */
    public static void sendWebAddressToServer(String webAddress) throws IOException {
        byte[] webServerURL = new byte[CHUNK_SIZE];
        webServerURL = webAddress.getBytes();
        DatagramPacket webServerPacket = new DatagramPacket(webServerURL, webServerURL.length, SERVER_ADDRESS, PORT);

        // send datagram to server
        clientSocket.send(webServerPacket);
    }

    /**
     * Receives the chunk of the web server response from the UDP server.
     * 
     * @return HashMap<String, Object>
     *         Contains the sequence number, payload, and whether it is the last
     *         chunk.
     * @throws IOException
     */
    public static HashMap<String, Object> receiveResponseFromServer() throws IOException {
        // Receive the response from the server
        // Sequence number + payload length + payload
        byte[] datagramBuffer = new byte[(Integer.BYTES * 3) + CHUNK_SIZE];
        DatagramPacket datagramFromServer = new DatagramPacket(datagramBuffer, datagramBuffer.length);
        clientSocket.receive(datagramFromServer);

        ByteBuffer serverBuffer = ByteBuffer.wrap(datagramFromServer.getData(), 0, datagramFromServer.getLength());

        int seqNum = serverBuffer.getInt();
        int payloadLength = serverBuffer.getInt();
        int isLastChunk = serverBuffer.getInt();
        byte[] payloadBytes = new byte[payloadLength];
        serverBuffer.get(payloadBytes);

        String payload = new String(payloadBytes);

        return new HashMap<String, Object>() {
            {
                put("seqNum", seqNum);
                put("payload", payload);
                put("isLastChunk", isLastChunk);
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

        // Checks if we've received the entirety of the response from the web server
        boolean fullResReceived = false;
        ByteArrayOutputStream fullRes = new ByteArrayOutputStream();
        while (!fullResReceived) {
            HashMap<String, Object> response = receiveResponseFromServer();

            // Create a Message object from the response that will be sent to the UDP server
            Message msg = new Message(
                    (Integer) response.get("seqNum"),
                    (String) response.get("payload"));

            System.out.println(
                    "Received chunk with sequence number: " + msg.getSeqNum() + ", length: " + msg.getLength());
            // Accumulate each chunk of the response
            fullRes.write(msg.getPayload().getBytes());

            // Allocate a buffer for the ACK datagram
            ByteBuffer ackBuffer = ByteBuffer.allocate(Integer.BYTES);

            // Set the sequence number for the ACK
            ackBuffer.putInt(msg.getSeqNum() ^ 1);

            DatagramPacket ackPacket = new DatagramPacket(
                    ackBuffer.array(),
                    Integer.BYTES,
                    SERVER_ADDRESS,
                    PORT);

            // Send ACK datagram to UDP server
            clientSocket.send(ackPacket);

            // Check if this is the last chunk
            // If the isLastChunk is 1, then we have received the full response and close
            // the client's socket
            if (response.get("isLastChunk").equals(1)) {
                fullResReceived = true;
                System.out.println("Full response: " + fullRes.toString());
            }
        }

        clientSocket.close();
    }
}
