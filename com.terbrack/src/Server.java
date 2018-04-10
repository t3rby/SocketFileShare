import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

public class Server {

    // private static HashMap<String, String> clients = new HashMap<>();
    private static HashMap<String, TreeSet<String>> files = new HashMap<>();
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
                System.out.println("Server has ended");
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
            System.out.println("Type 'quit' to shut down the server");
        }

        // Close server socket
        System.out.println("Shutting down server...");
        serverSocket.close();
    }

    // Thread to handle a client's connection
    private static class Handler extends Thread {

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected to client");

            try {
                // Initialize streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Main processing loop
                String line;
                String[] tokens;
                while ((line = in.readLine()) != null) {
                    tokens = line.split(" ");
                    switch (tokens[0]) {
                        case "FILESHARE":
                            addFile(tokens[1], tokens[2]);
                            break;
                        case "FILEREQUEST":
                            if (files.containsKey(tokens[1])) {
                                sendFileInfo(tokens[1]);
                            } else {
                                out.println("NOCLIENTS");
                            }
                            break;
                        case "FILELIST":
                            String files = createFileList();
                            out.println(files);
                            break;
                        case "QUIT":
                            System.out.println("Client disconnected");
                            return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Create a list of all the files available
        private String createFileList() {
            StringBuilder filesBuilder = new StringBuilder();
            for (String file : files.keySet()) {
                filesBuilder.append(file);
                filesBuilder.append(" ");
            }
            return filesBuilder.toString();
        }

        // Add a file to the file list
        private void addFile(String fileName, String port) {
            if (files.containsKey(fileName)) {
                files.get(fileName).add(socket.getInetAddress() + " " + port);
            } else {
                files.put(fileName, new TreeSet<>());
                files.get(fileName).add(socket.getInetAddress() + " " + port);
            }
        }

        // Send address where requested file can be found
        private void sendFileInfo(String fileName) throws IOException {
            TreeSet<String> clients = files.get(fileName);
            Iterator<String> iterator = clients.iterator();
            while (iterator.hasNext()) {
                out.println(iterator.next());
                if (in.readLine().equals("SUCCESS")) {
                    return;
                } else {
                    iterator.remove();
                }
            }
            out.println("NOCLIENTS");
            files.remove(fileName);
        }
    }
}
