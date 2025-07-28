import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;


class ClientHandler implements Runnable {
    private String webAddress; // Web address requested by client
    private InetAddress clientIP; // Client's IP address
    private int clientPort; // Client's port
	private int timeout; //ts

    public ClientHandler(String webAddress, InetAddress clientIP, int clientPort, int timeout) {
        this.webAddress = webAddress;
        this.clientIP = clientIP;
        this.clientPort = clientPort;
		this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            //Fetch HTML content from the requested website
            URI uri = new URI("https://" + webAddress);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream inputStream = conn.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int bytesRead;

            //Read webpage in chunks and store in memory
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }

            byte[] fullPage = buffer.toByteArray();
            System.out.println("Fetched " + fullPage.length + " bytes from " + webAddress);

            //Setup UDP socket for sending data to client
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout * 1000); //ts

            int chunkSize = 1024; 
            int totalBytes = fullPage.length;
            int offset = 0;
            int seqNum = 0; // Start sequence number (0 or 1 alternating)

            //Stop and Wait. Send one chunk at a time
            while (offset < totalBytes) {
                int bytesRemaining = totalBytes - offset;
                int currentChunkSize = Math.min(chunkSize, bytesRemaining);

                // Extract chunk to send
                byte[] payload = Arrays.copyOfRange(fullPage, offset, offset + currentChunkSize);

                //Build packet: [4 bytes seqNum] + [4 bytes payloadLength] + [payload]
                ByteBuffer sendBuffer = ByteBuffer.allocate(8 + payload.length);
                sendBuffer.putInt(seqNum);
                sendBuffer.putInt(payload.length);
                sendBuffer.put(payload);

                DatagramPacket packet = new DatagramPacket(
                        sendBuffer.array(), sendBuffer.array().length, clientIP, clientPort);

                boolean ackReceived = false;

                //Keep sending this chunk until ACK is received
                while (!ackReceived) {
                    // Send data packet to client
                    socket.send(packet);
                    System.out.println("Sent chunk seq=" + seqNum);

                    //Prepare to receive ACK
                    byte[] ackData = new byte[4];
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);

                    try {
                        socket.receive(ackPacket);
                        int ackNum = ByteBuffer.wrap(ackPacket.getData()).getInt();

                        //If ACK matches expected next sequence, mark as received
                        if (ackNum == (seqNum ^ 1)) {
                            ackReceived = true;
                            offset += currentChunkSize; //Move to next chunk
                            seqNum ^= 1; //Toggle sequence number
                        } else {
                            System.out.println("ACK mismatch. Resending seq=" + seqNum);
                        }
                    } catch (SocketTimeoutException e) {
                        //Timeout occurred, resend current chunk
                        System.out.println("Timeout waiting for ACK. Resending seq=" + seqNum);
                    }
                }
            }

            //Finished sending all data
            System.out.println("Sent entire page to client (" + fullPage.length + " bytes)");
            socket.close();

        } catch (IOException | URISyntaxException e) {
            System.err.print("Error: " + e.getMessage());
        }
    }
}

public class NetServerV2 {
    public static void main(String[] args) throws Exception {

        // Create UDP server socket listening on port 11122
        DatagramSocket serverSocket = new DatagramSocket(11122);

        // Ensure socket is closed gracefully when server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> serverSocket.close()));

		//get ts from client
		Scanner s = new Scanner(System.in);
		System.out.print("Enter timeout in secs: ");
		int timeout = s.nextInt();
		s.close();

        byte[] receiveData = new byte[1024];

        while (true) {
            // Wait for client request
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            // Extract the requested web address from the packet
            String webAddress = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received: " + webAddress);

            // Start a new thread to handle this client request
            new Thread(new ClientHandler(webAddress, receivePacket.getAddress(), receivePacket.getPort(), timeout)).start();
        }
    }
}

