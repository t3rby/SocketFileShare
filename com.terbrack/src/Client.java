import javax.swing.*;

public class Client {

    private JList availableFileList;
    private JPanel rootPanel;
    private JList userFileList;
    private JTextField userDirectoryField;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Client");
        frame.setContentPane(new Client().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
