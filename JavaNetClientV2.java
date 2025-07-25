import java.util.Scanner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class JavaNetClientV2 {
    public static void main(String[] args) {
        Scanner myScanner = new Scanner(System.in);
        System.out.println("Enter a web address");
        String webServer = myScanner.nextLine();
        String host = "127.0.0.1";
        int port = 11122;

        try (Socket client = new Socket()) {
            client.connect(new InetSocketAddress(host, port));
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
