import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final int FILE_SIZE = 6022386;

    private static ServerSocket serverSocket;
    private static String directory;

    // Thread to continually accept new client connections
    private static Thread acceptThread = new Thread("Accept Thread") {
        @Override
        public void run() {
            try {
                while (true) {
                    new Handler(serverSocket.accept()).start();
                }
            } catch (IOException e) {
                System.out.println("Client has ended");
            }
        }
    };


    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        // Get server information
        System.out.print("Enter server IP address: ");
        String address = sc.nextLine();
        System.out.print("Enter server port: ");
        int port = Integer.parseInt(sc.nextLine());

        // Set up server streams
        Socket socket = new Socket(address, port);
        BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);

        // Set up client listener
        System.out.print("Enter listener port: ");
        port = Integer.parseInt(sc.nextLine());
        serverSocket = new ServerSocket(port);
        acceptThread.start();

        // Choose directory where files will be shared and stored
        System.out.print("Enter directory for file sharing and storage: ");
        directory = sc.nextLine();
        System.out.println("Current directory: " + directory);

        // Main input processing loop
        String line;
        String[] tokens;
        while (!(line = sc.nextLine()).equals("quit")) {
            tokens = line.split(" ");
            switch (tokens[0]) {
                // Tell server that file can be shared
                case "share":
                    serverOut.println("FILESHARE " + tokens[1] + " " + serverSocket.getLocalPort());
                    System.out.println("Shared file: " + directory + "/" + tokens[1]);
                    break;

                // Get list of available files from server
                case "files":
                    serverOut.println("FILELIST");
                    String files = serverIn.readLine();
                    tokens = files.split(" ");
                    System.out.println("Files available:");
                    for (String fileName : tokens) {
                        System.out.println(fileName);
                    }
                    break;

                // Get a file from a client
                case "get":
                    String fileName = tokens[1];
                    serverOut.println("FILEREQUEST " + fileName);
                    String clientInfo;
                    while (!(clientInfo = serverIn.readLine()).equals("NOCLIENTS")) {
                        tokens = clientInfo.split(" ");
                        if (getFile(fileName, tokens[0].substring(1), Integer.parseInt(tokens[1]))) {
                            serverOut.println("SUCCESS");
                            break;
                        } else {
                            serverOut.println("FILEREQUEST " + fileName);
                        }
                    }
                    if (clientInfo.equals("NOCLIENTS")) {
                        System.out.println("No clients were found");
                    }
                    break;

                default:
                    System.out.println("Unknown command");
                    break;
            }
        }

        // Close listener socket
        serverOut.println("QUIT");
        serverSocket.close();
    }

    // Get a file from another client
    private static boolean getFile(String fileName, String address, int port) {
        try {
            // Set up client connection
            System.out.println("Address: " + address + " Port: " + port);
            Socket clientSocket = new Socket(address, port);
            InputStream clientIn = clientSocket.getInputStream();
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send request for file
            System.out.println("Requesting file: " + fileName);
            clientOut.println(fileName);

            // Receive file
            byte[] fileByteArray = new byte[FILE_SIZE];
            String filePath = directory + "/" + fileName;
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            int bytesRead = clientIn.read(fileByteArray, 0, fileByteArray.length);
            int current = bytesRead;
            do {
                bytesRead = clientIn.read(fileByteArray, current, (fileByteArray.length - current));
                if (bytesRead >= 0) {
                    current += bytesRead;
                }
            } while (bytesRead > -1);
            bos.write(fileByteArray, 0, current);
            bos.flush();
            System.out.println("File received: " + filePath);

            // Close streams and socket
            bos.close();
            clientIn.close();
            clientOut.close();
            clientSocket.close();
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    // Thread to handle a client file request connection
    private static class Handler extends Thread {

        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("New client connected");

            try {
                // Set up client streams
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream clientOut = socket.getOutputStream();

                // Send file
                String fileName = clientIn.readLine();
                System.out.println("Received request for " + fileName);
                String filePath = directory + "/" + fileName;
                File file = new File(filePath);
                byte[] fileByteArray = new byte[(int)file.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(fileByteArray, 0, fileByteArray.length);
                clientOut.write(fileByteArray, 0, fileByteArray.length);
                clientOut.flush();
                System.out.println("File sent: " + filePath);

                // Close streams and socket
                bis.close();
                clientIn.close();
                clientOut.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
