import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NetServerV2 extends Thread {

	public static void main(String[] args) {
		Scanner myScanner = new Scanner(System.in);
		String host = "127.0.0.1";
		int port = 11122;
		int timeout = 5;

		try {
			ServerSocket serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(host, port));

			while (true) {
				Socket clientSocket = serverSocket.accept();
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
