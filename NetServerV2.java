import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

class ClientHandler implements Runnable {
	private String webAddress;
	private InetAddress clientIP;
	private int clientPort;
	private DatagramSocket socket;
	private int timeout;

	public ClientHandler(String webAddress, InetAddress clientIP, int clientPort, int timeout) throws SocketException {
		this.webAddress = webAddress;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.timeout = timeout;
		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(this.timeout);
	}

	public byte[] getWebServerRes() throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
				// In cases where you get a 301, which results in the response returning nothing
				.followRedirects(HttpClient.Redirect.ALWAYS)
				.build();

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create("https://" + this.webAddress))
				.build();

		byte[] resBody;
		HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
		System.out.println("Status: " + res.statusCode());
		resBody = res.body();
		return resBody;
	}

	public void sendResToClient(byte[] data) throws IOException, InterruptedException {
		// Send data in chunks to avoid packet size issues
		int chunkSize = 1024;
		int totalBytes = data.length;
		int offset = 0;
		int seqNum = 0;

		while (offset < totalBytes) {
			int bytesRemaining = totalBytes - offset;
			int currentChunkSize = Math.min(chunkSize, bytesRemaining);

			// Extract the chunk of payload
			byte[] payload = Arrays.copyOfRange(data, offset, offset + currentChunkSize);

			// Stores the message in a byte buffer
			// 4 bytes for seqNum:int
			// 4 bytes for payload length:int
			// Whatever the payload length is
			ByteBuffer buffer = ByteBuffer.allocate((Integer.BYTES * 2) + payload.length);
			buffer.putInt(seqNum);
			buffer.putInt(payload.length);
			buffer.put(payload);
			// create packet with a chunk of res.body of size currentChunkSize
			DatagramPacket packet = new DatagramPacket(
					buffer.array(),
					buffer.array().length,
					clientIP,
					clientPort);

			boolean receivedAck = false;
			while (!receivedAck) {
				// Send out to client
				socket.send(packet);

				// Wait for ACK from client, which is an int of 4 bytes
				byte[] ackBuffer = new byte[4];
				DatagramPacket ackPacket = new DatagramPacket(
						ackBuffer,
						ackBuffer.length);

				try {
					socket.receive(ackPacket);
					ByteBuffer ackByteBuffer = ByteBuffer.wrap(ackPacket.getData());
					int clientSeqNum = ackByteBuffer.getInt();

					if (clientSeqNum == (seqNum ^ 1)) {
						receivedAck = true;
						offset += currentChunkSize;
						// Alternate sequence number for next chunk
						seqNum ^= 1;
					} else {
						System.out.println("ACK mismatch. Resending chunk " + (offset / chunkSize + 1));
					}
				} catch (SocketTimeoutException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			byte[] serverResponse = getWebServerRes();
			sendResToClient(serverResponse);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

public class NetServerV2 {
	public static void main(String[] args) throws Exception {

		// Scanner scanner = new Scanner(System.in);
		// System.out.println("Enter a timeout in seconds for the server to wait for a
		// client request:");
		// int timeout = scanner.nextInt();
		int timeout = 10;
		// scanner.close();

		DatagramSocket serverSocket = new DatagramSocket(11122);

		// Close server socket on SIGTERM
		Runtime.getRuntime().addShutdownHook(new Thread(
				() -> {
					System.out.println("\nShutting down server...");
					serverSocket.close();
				}));

		while (true) {
			try {
				byte[] receiveData = new byte[1024];
				DatagramPacket clientPacket = new DatagramPacket(receiveData, receiveData.length);
				// Anticipate client request
				serverSocket.receive(clientPacket);
				System.out.println(clientPacket.getPort());
				// byte [] to String conversion
				String webAddress = new String(
						clientPacket.getData(),
						0,
						clientPacket.getLength());

				System.out.println("Received: " + webAddress);

				ClientHandler client = new ClientHandler(webAddress,
						clientPacket.getAddress(),
						clientPacket.getPort(),
						timeout);

				// Start a new thread to handle the client request
				new Thread(client).start();
			} catch (SocketException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
