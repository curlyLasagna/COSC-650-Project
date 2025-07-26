import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NetServerV2 extends Thread {

	final static String HOST = "127.0.0.1";
	final static int PORT = 11122;

	public static String sendGetRequests(String webServer) throws IOException, InterruptedException {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(webServer))
					.build();

			HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
			return res.body();
		}
	}

	public static void main(String[] args) {
		Scanner myScanner = new Scanner(System.in);
		int timeout = 5;

		try {
			System.out.println(sendGetRequests("https://www.reddit.com/"));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			ServerSocket serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(HOST, PORT));

			while (true) {
				Socket clientSocket = serverSocket.accept();
				// clientSocket.getOutputStream().write(sendGet.getBytes());
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
