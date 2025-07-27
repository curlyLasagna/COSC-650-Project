import java.io.*;
import java.net.*;
import java.util.Arrays;


public class NetServerV2 {
    public static void main(String[] args) throws Exception {

        DatagramSocket serverSocket = new DatagramSocket(11122);

        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];

        while (true) {
            //wait for message
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            //get client input
            String webAddress = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received: " + webAddress);

			new Thread(new ClientHandler(webAddress, receivePacket.getAddress(), receivePacket.getPort())).start();
		}
	}
}

class ClientHandler implements Runnable{
	private String webAddress;
	private InetAddress clientIP;
	private int clientPort;

	public ClientHandler(String webAddress, InetAddress clientIP, int clientPort){
		this.webAddress = webAddress;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
	}
	@Override
	public void run(){
		try {
			// fetch html content from web address
			// TODO: Break into a separate function that returns the body
			URL url = new URL("https://" + webAddress);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			//read response
			InputStream inputStream = conn.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[1024];
			int bytesRead;
			//read in chunks
			while((bytesRead = inputStream.read(chunk)) != -1){
				buffer.write(chunk, 0, bytesRead);
			}
			byte[] fullPage = buffer.toByteArray();
			System.out.println("Fetched " +  fullPage.length + " bytes from " + webAddress);

			DatagramSocket socket = new DatagramSocket();
			int chunkSize = 1024; 
			int totalBytes = fullPage.length;
			int offset = 0;

			//loop until all bytes sent
			while(offset < totalBytes){

				int bytesRemaining = totalBytes - offset;
				int currentChunkSize = Math.min(chunkSize, bytesRemaining);

				//extract chunk to send in this packet
				byte[] payload = Arrays.copyOfRange(fullPage, offset, offset + currentChunkSize);

				//create packet with chunk 
				DatagramPacket packet = new DatagramPacket(payload, payload.length, clientIP, clientPort);

				// Send out to client
				socket.send(packet);

				System.out.println("Sent chunk " + (offset / chunkSize + 1));
				offset += currentChunkSize;

				try {
    				Thread.sleep(30);
				} 
				catch (InterruptedException e) {
    				e.printStackTrace();
				}

			}
		
			System.out.println("Sent entire page to client" + fullPage.length + " bytes");

			socket.close();
		}
		catch (IOException e){
			System.err.print("Error: " + e.getMessage());
		}
	}
}
			  
