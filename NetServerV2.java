import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NetServerV2 extends Thread {

	public class ClientListener {

	}

	public static void main(String[] args) {
		Scanner myScanner = new Scanner(System.in); // Create a Scanner object
		System.out.println("Enter the IP address of the server, Enter for loopback");
		String host = myScanner.nextLine();
		if (host.isEmpty()) {
			host = "127.0.0.1";
		}

		// Default port of 11122
		int port = 11122;
		System.out.println("Enter timeout period");
		if (myScanner.nextLine().isEmpty()) {
		}

		try {
			ServerSocket serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(host, port));
			System.out.println("Server: Listening on " + host + ":" + port);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Connected from: " + clientSocket.getRemoteSocketAddress());
				clientSocket.getOutputStream().write("Hello Client".getBytes());
				clientSocket.close();
				System.exit(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		myScanner.close();
	}
}
