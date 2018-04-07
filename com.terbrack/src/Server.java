import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class Server {

    private static HashMap<String, PrintWriter> clients = new HashMap<>();
    private static ServerSocket serverSocket;

    // Thread to continually accept new client connections
    private static Thread acceptThread = new Thread("Accept Thread") {
        @Override
        public void run() {
            try {
                while (true) {
                    new Handler(serverSocket.accept()).start();
                }
            } catch (IOException e) {
                System.out.println("Server ended.");
            }
        }
    };

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        // Set up server socket
        System.out.print("Enter port: ");
        int port = Integer.parseInt(sc.nextLine());
        serverSocket = new ServerSocket(port);
        System.out.println("Server running...");

        // Start accepting client connections
        acceptThread.start();

        // Wait for quit command
        while (!sc.nextLine().equals("quit")) {
            System.out.println("Type 'quit' to shut down the server.");
        }

        // Close server socket
        System.out.println("Shutting down server...");
        serverSocket.close();
    }

    // Thread to handle a client's connection
    private static class Handler extends Thread {

        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected to client.");

            try {
                // Initialize streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Get username and add to client list
                while (true) {
                    out.println("GETUSER");
                    String response = in.readLine();
                    if (!clients.containsKey(response)) {
                        username = response;
                        clients.put(username, out);
                        out.println("ACCEPTUSER " + username);
                        break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
