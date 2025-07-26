import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NetServerV2 extends Thread {

	final static String HOST = "127.0.0.1";
	final static int PORT = 11122;

	public static void main(String[] args) {
		Scanner myScanner = new Scanner(System.in);
		int timeout = 5;

		try {
			ServerSocket serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(HOST, PORT));

			while (true) {
				Socket clientSocket = serverSocket.accept();
				clientSocket.getOutputStream().write("Hello Client".getBytes());
				clientSocket.close();
				serverSocket.close();
				System.exit(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		myScanner.close();
	}
}
