import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class JavaNetClientV2 {
    public static void main(String[] args) {
		Scanner myScanner = new Scanner(System.in);  // Create a Scanner object
		System.out.println("Enter the IP address of the server, Enter for loopback");
		String host = myScanner.nextLine();
		if (host.isEmpty()) {
			host = "127.0.0.1";
		}
		
		int port = 42181;
		System.out.println("Enter the port number that the server is listening on, Enter for 42181");
		String sPort = myScanner.nextLine();
		if (!sPort.isEmpty()) {
			port = Integer.parseInt(sPort);
		}

        try (Socket client = new Socket()) {
            client.connect(new InetSocketAddress(host, port));
            System.out.println("Connected to Server: " + host + " on " + port);

            byte[] buffer = new byte[1024];
            int bytesRead = client.getInputStream().read(buffer);
            if (bytesRead > 0) {
                System.out.println("Received: " + new String(buffer, 0, bytesRead));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Client: Disconnected from Server");
    }
}

