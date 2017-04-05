package handin;

import exercise3.AbstractClient;
import exercise3.AbstractServer;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;

public class DistributedTextEditor extends JFrame {

    private Action Disconnect;
    private JTextArea area1;
    private ActionMap m;
    private Action Copy;
    private Action Paste;
    private JTextArea area2;
    private JTextField ipAddress;
    private JTextField portNumber;
    private EventReplayer er;
    private Thread ert;
    private JFileChooser dialog;
    private String currentFile = "Untitled";
    private boolean changed = false;
    private Action Save;
    private Action SaveAs;
    private Action Listen;
    private Action Connect;
    private Action Quit;
    private boolean connected = false;
    private DocumentEventCapturer dec = new DocumentEventCapturer();
    private KeyListener k1;

    public DistributedTextEditor() {

        area1 = new JTextArea(20, 120);
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));

        area2 = new JTextArea(20, 120);
        area2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(dec);
        area2.setEditable(false);

        initializeActions();

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1, BorderLayout.CENTER);

        JScrollPane scroll2 =
                new JScrollPane(area2,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll2, BorderLayout.CENTER);

        ipAddress = new JTextField("localhost");
        content.add(ipAddress, BorderLayout.CENTER);
        portNumber = new JTextField("40499");
        content.add(portNumber, BorderLayout.CENTER);

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);


        file.add(Listen);
        file.add(Connect);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);


        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        k1 = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                changed = true;
                Save.setEnabled(true);
                SaveAs.setEnabled(true);
            }
        };
        area1.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
        area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
                "Try to type and delete stuff in the top area.\n" +
                "Then figure out how it works.\n", 0);

        er = new EventReplayer(dec, area2);
        ert = new Thread(er);
        ert.start();
        dialog = new JFileChooser(System.getProperty("user.dir"));
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

    private void initializeActions() {
        Disconnect = new AbstractAction("Disconnect") {
            public void actionPerformed(ActionEvent e) {
                setTitle("Disconnected");
                // TODO
            }
        };
        Save = new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) {
                if (!currentFile.equals("Untitled"))
                    saveFile(currentFile);
                else
                    saveFileAs();
            }
        };
        SaveAs = new AbstractAction("Save as...") {
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        };
        Quit = new AbstractAction("Quit") {
            public void actionPerformed(ActionEvent e) {
                saveOld();
                System.exit(0);
            }
        };
        m = area1.getActionMap();
        Copy = m.get(DefaultEditorKit.copyAction);
        Paste = m.get(DefaultEditorKit.pasteAction);

        Listen = new AbstractAction("Listen") {
            public void actionPerformed(ActionEvent e) {
                saveOld();
                area1.setText("");
                new Thread(() -> {
                    AbstractServer server = new AbstractServer(getPortNumber());
                    server.registerOnPort();
                    setTitle("I'm listening on " + server.getLocalHostAddress() + " on port " + getPortNumber());
                    Socket socket = server.waitForConnectionFromClient();

                    try {
                        final ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                        while (socket.isConnected()) {
                            Object o = fromClient.readObject();
                            if (o instanceof MyTextEvent) {
                                MyTextEvent event = (MyTextEvent) o;
                                dec.addMyTextEvent(event);
                            } else {
                                System.out.println("Unreadable object reveived");
                            }
                        }
                        fromClient.close();

                    } catch (EOFException ex) {
                        System.out.println("Connection to client was broken");
                    } catch (IOException | ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }


                }).start();

                changed = false;
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
            }
        };

        Connect = new AbstractAction("Connect") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    saveOld();
                    area1.setText("");
                    AbstractClient client = new AbstractClient(getPortNumber());
                    Socket socket = client.connectToServer(getIP());
                    setTitle("Connected to " + getIP() + " on port " + getPortNumber());

                    final ObjectOutputStream outputStream;
                    try {
                        outputStream = new ObjectOutputStream(socket.getOutputStream());
                        while (socket.isConnected()) {
                            try {
                                outputStream.writeObject(dec.take());
                            } catch (IOException | InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }).start();

            }
        };
    }

    private int getPortNumber() {
        return Integer.parseInt(portNumber.getText());
    }

    private String getIP() {
        return ipAddress.getText();
    }

    private void saveFileAs() {
        if (dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if (changed) {
            if (JOptionPane.showConfirmDialog(this, "Would you like to save " + currentFile + " ?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        } catch (IOException e) {
        }
    }

}
