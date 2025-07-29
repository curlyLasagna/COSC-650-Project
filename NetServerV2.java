import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

class ClientHandler implements Runnable {
	private String webAddress;
	private InetAddress clientIP;
	private int clientPort;
	private int timeout;
	private DatagramSocket socket;
	private BlockingQueue<DatagramPacket> ackQueue = new LinkedBlockingQueue<>();

	public ClientHandler(String webAddress, InetAddress clientIP, int clientPort, int timeout, DatagramSocket socket)
			throws SocketException {
		this.webAddress = webAddress;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.timeout = timeout;
		this.socket = socket;
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
		resBody = res.body();
		return resBody;
	}

	public void receiveAck(DatagramPacket ackPacket) {
		ackQueue.offer(ackPacket);
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

			int isLastChunk = (bytesRemaining <= chunkSize) ? 1 : 0;

			// Stores the message in a byte buffer
			// 4 bytes for seqNum:int
			// 4 bytes for payload length:int
			// Whatever the payload length is
			ByteBuffer buffer = ByteBuffer.allocate((Integer.BYTES * 3) + payload.length);
			buffer.putInt(seqNum);
			buffer.putInt(payload.length);
			buffer.putInt(isLastChunk);
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

				try {
					// Wait for ACK from client, which is an int of 4 bytes
					DatagramPacket ackPacket = ackQueue.poll(timeout, java.util.concurrent.TimeUnit.SECONDS);

					// if there's an ACK packet in the queue, read the sequence number to determine
					// if there's an ACK mismatch
					if (ackPacket != null) {
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
					} else {
						System.out.println("No ACK received. Resending chunk " + (offset / chunkSize + 1));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		System.out.println("All chunks sent successfully to client: " + clientIP + ":" + clientPort);
	}

	@Override
	public void run() {
		try {
			byte[] serverResponse = getWebServerRes();
			sendResToClient(serverResponse);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			NetServerV2.removeClient(clientPort);
		}
	}
}

public class NetServerV2 {

	public static void removeClient(int clientKey) {
		clientHandlers.remove(clientKey);
	}

	// Map to hold client handlers by their port number
	private static final ConcurrentHashMap<Integer, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

	public static void main(String[] args) throws Exception {

		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter a timeout in seconds for the server to wait for a client request:");
		int timeout = scanner.nextInt();
		scanner.close();

		DatagramSocket serverSocket = new DatagramSocket(11122);

		// Close server socket on SIGTERM
		Runtime.getRuntime().addShutdownHook(new Thread(
				() -> {
					System.out.println("\nShutting down server...");
					serverSocket.close();
				}));

		// Listen for incoming client requests
		while (true) {
			byte[] receiveData = new byte[1024];
			DatagramPacket clientPacket = new DatagramPacket(receiveData, receiveData.length);
			// Anticipate client request
			serverSocket.receive(clientPacket);

			// Populate map of client state with client key
			int clientKey = clientPacket.getPort();
			ClientHandler clientHandler;
			// Route an ACK packet to the proper thread
			if (clientPacket.getLength() == 4) {
				clientHandler = clientHandlers.get(clientKey);
				clientHandler.receiveAck(clientPacket);
			} else {
				// byte [] to String conversion
				String webAddress = new String(
						clientPacket.getData(),
						0,
						clientPacket.getLength());

				System.out.println("Received: " + webAddress);

				clientHandler = new ClientHandler(webAddress,
						clientPacket.getAddress(),
						clientPacket.getPort(),
						timeout,
						serverSocket);

				clientHandlers.put(clientKey, clientHandler);
				// Start a new thread to handle the client request
				new Thread(clientHandler).start();
			}
		}
	}
}
