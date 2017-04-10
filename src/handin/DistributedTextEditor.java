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
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class DistributedTextEditor extends JFrame {

    private boolean changed = false;

    private JTextArea area1;
    private JTextArea area2;
    private JTextField ipAddress;
    private JTextField portNumber;
    private JFileChooser dialog;
    private String currentFile = "Untitled";

    private Action Disconnect;
    private Action Copy;
    private Action Paste;
    private Action Save;
    private Action SaveAs;
    private Action Listen;
    private Action Connect;
    private Action Quit;

    private DocumentEventCapturer inputDec = new DocumentEventCapturer();
    private DocumentEventCapturer outputDec = new DocumentEventCapturer();

    private Thread localReplayThread;
    private Thread onlineReplayThread;

    private Socket socket;
    private boolean online;

    public DistributedTextEditor() {

        area1 = new JTextArea(20, 120);
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));

        area2 = new JTextArea(20, 120);
        area2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(inputDec);
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
        Disconnect.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        KeyListener k1 = new KeyAdapter() {
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

        //initialize the replayer on area2
        EventReplayer localReplayer = new EventReplayer(inputDec, new LocalOutputStrategy(area2));
        localReplayThread = new Thread(localReplayer);
        localReplayThread.start();

        dialog = new JFileChooser(System.getProperty("user.dir"));
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

    /**
     * Sets the menuButtons related to online.
     *
     * @param online whether the new state is online or offline
     */
    private void updateConnectionMenuButtons(boolean online) {
        Listen.setEnabled(!online);
        Connect.setEnabled(!online);
        Disconnect.setEnabled(online);
    }

    private void initializeActions() {
        Disconnect = new AbstractAction("Disconnect") {
            public void actionPerformed(ActionEvent e) {

                // Resets the online connections
                online = false;
                if (onlineReplayThread != null) {
                    onlineReplayThread.interrupt();
                }
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                //sets the Eventreplayer to offline mode
                UpdateLocalReplayer(inputDec);

                //resets the ui:
                area1.setText("");
                area2.setText("");
                updateConnectionMenuButtons(false);
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
        ActionMap m = area1.getActionMap();
        Copy = m.get(DefaultEditorKit.copyAction);
        Paste = m.get(DefaultEditorKit.pasteAction);

        Listen = new AbstractAction("Listen") {
            public void actionPerformed(ActionEvent e) {
                saveOld();
                area1.setText("");
                updateConnectionMenuButtons(true);

                //sets the EventReplayer to online mode
                UpdateLocalReplayer(outputDec);

                new Thread(() -> {
                    AbstractServer server = new AbstractServer(getPortNumber());
                    server.registerOnPort();
                    setTitle("I'm listening on " + server.getLocalHostAddress() + " on port " + getPortNumber());
                    online = true;
                    //listen for new clients, until user "disconnects"
                    while (online) {
                        socket = server.waitForConnectionFromClient();
                        receiveEvents(socket);
                    }
                    server.deregisterOnPort();
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
                    //sets the EventReplayer to online mode
                    UpdateLocalReplayer(outputDec);

                    updateConnectionMenuButtons(true);
                    saveOld();
                    area1.setText("");

                    AbstractClient client = new AbstractClient(getPortNumber());
                    socket = client.connectToServer(getIP());

                    if (socket == null) {
                        startOfflineMode();
                        setTitle("connection failed - Disconnected");
                        return;
                    }

                    setTitle("Connected to " + getIP() + " on port " + getPortNumber());

                    receiveEvents(socket);
                }).start();

                changed = false;
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
            }
        };
    }

    private void startOfflineMode() {
        UpdateLocalReplayer(inputDec);
        updateConnectionMenuButtons(false);
    }

    private void UpdateLocalReplayer(DocumentEventCapturer dec) {
        localReplayThread.interrupt();
        EventReplayer localReplayer = new EventReplayer(dec, new LocalOutputStrategy(area2));
        localReplayThread = new Thread(localReplayer);
        localReplayThread.start();
    }

    private void receiveEvents(Socket socket) {
        onlineReplayThread = new Thread(
                new EventReplayer(inputDec, new RemoteOutputStrategy(socket))
        );
        onlineReplayThread.start();

        try {

            final ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());

            while (socket.isConnected() && !socket.isClosed()) {
                Object o = fromClient.readObject();
                if (o instanceof MyTextEvent) {
                    MyTextEvent event = (MyTextEvent) o;
                    outputDec.addMyTextEvent(event);
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

        onlineReplayThread.interrupt();
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
