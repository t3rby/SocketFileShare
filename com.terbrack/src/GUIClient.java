import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class GUIClient {
    private JFrame frame = new JFrame("SocketFileShare");
    private JPanel top;
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JTextArea directoryTextField;
    private JTextField shareTextField;
    private JButton refreshButton;

    private static final int FILE_SIZE = 6022386;

    private BufferedReader serverIn;
    private PrintWriter serverOut;
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


    public GUIClient() {
        // Initialize GUI elements
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        directoryTextField = new JTextArea();
        shareTextField = new JTextField();
        refreshButton = new JButton("Refresh");

        // Set up GUI
        directoryTextField.setEditable(false);
        directoryTextField.setOpaque(false);
        top = new JPanel();
        top.setLayout(new BorderLayout());
        top.add(directoryTextField, "West");
        top.add(refreshButton, "East");
        frame.getContentPane().add(top, "North");
        frame.getContentPane().add(new JScrollPane(fileList), "Center");
        frame.getContentPane().add(shareTextField, "South");
        frame.pack();

        // Double click on file
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    try {
                        String fileName = fileList.getSelectedValue();
                        serverOut.println("FILEREQUEST " + fileName);
                        String clientInfo;
                        while (!(clientInfo = serverIn.readLine()).equals("NOCLIENTS")) {
                            String[] tokens = clientInfo.split(" ");
                            if (getFile(fileName, tokens[0].substring(1), Integer.parseInt(tokens[1]))) {
                                serverOut.println("SUCCESS");
                                break;
                            } else {
                                serverOut.println("FILEREQUEST " + fileName);
                            }
                        }
                        if (clientInfo.equals("NOCLIENTS")) {
                            shareTextField.setText("No clients found");
                            refreshButton.doClick();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Press enter in share field
        shareTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverOut.println("FILESHARE " + shareTextField.getText() + " " + serverSocket.getLocalPort());
                shareTextField.setText("");
                refreshButton.doClick();
            }
        });

        // Press the refresh button
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    fileListModel.clear();
                    serverOut.println("FILELIST");
                    String files = serverIn.readLine();
                    String[] tokens = files.split(" ");
                    for (String fileName : tokens) {
                        fileListModel.addElement(fileName);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Close client
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    serverOut.println("QUIT");
                    serverSocket.close();
                    System.exit(0);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    private boolean getFile(String fileName, String address, int port) {
        try {
            // Set up client connection
            Socket clientSocket = new Socket(address, port);
            InputStream clientIn = clientSocket.getInputStream();
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send request for file
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
            shareTextField.setText("File received: " + filePath);

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

    private void run() {

        // Get IP address, ports, and directory
        String serverAddress = JOptionPane.showInputDialog(
                null,
                "Server IP Address:",
                "127.0.0.1"
        );
        int serverPort = Integer.parseInt(JOptionPane.showInputDialog(
                null,
                "Server Port:",
                "38000"
        ));
        int listenerPort = Integer.parseInt(JOptionPane.showInputDialog(
                null,
                "Listener Port:",
                "39000"
        ));
        directory = JOptionPane.showInputDialog(
                null,
                "Directory:",
                "c:/Users/tyler"
        );

        // Set up text
        directoryTextField.setText(directory);
        frame.setTitle(serverAddress + ":" + serverPort);

        try {
            // Set up server streams
            Socket socket = new Socket(serverAddress, serverPort);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverOut = new PrintWriter(socket.getOutputStream(), true);

            // Set up listener
            serverSocket = new ServerSocket(listenerPort);
            acceptThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GUIClient client = new GUIClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }

    // Thread to handle a client file request connection
    private static class Handler extends Thread {

        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Set up client streams
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream clientOut = socket.getOutputStream();

                // Send file
                String fileName = clientIn.readLine();
                String filePath = directory + "/" + fileName;
                File file = new File(filePath);
                byte[] fileByteArray = new byte[(int)file.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(fileByteArray, 0, fileByteArray.length);
                clientOut.write(fileByteArray, 0, fileByteArray.length);
                clientOut.flush();

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
