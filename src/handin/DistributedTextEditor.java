package handin;

import exercise3.Client;
import exercise3.Server;
import handin.output_strategy.LocalOutputStrategy;
import handin.output_strategy.RemoteOutputStrategy;
import handin.text_events.MyTextEvent;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class DistributedTextEditor extends JFrame {

    private static final int SECOND_WINDOW_POSITION = 700;
    private static Path posFile;
    private boolean changed = false;

    private JTextArea area1;
    private JTextArea area2;
    private JTextField ipAddress;
    private JTextField portNumber;
    private JFileChooser dialog;
    private String currentFile = "Untitled";
    private Server server;
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

    private Socket socket;
    private boolean listening;

    private DistributedTextEditor(int x, int y) {

        area1 = new JTextArea(12, 70);
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));

        area2 = new JTextArea(12, 70);
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

        this.setLocation(x, y);

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

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                removePosFile();
                super.windowClosing(e);
            }
        });
    }

    public static void main(String[] arg) throws IOException {
        String visited = "true";
        posFile = Paths.get(".pos");

        int x;

        if (!posFile.toFile().exists() || !Files.readAllLines(posFile).get(0).equals(visited)) {
            x = 0;
            List<String> lines = Collections.singletonList(visited);
            Files.write(posFile, lines, Charset.forName("UTF-8"));
        } else {
            x = SECOND_WINDOW_POSITION;
            List<String> lines = Collections.singletonList("");
            Files.write(posFile, lines, Charset.forName("UTF-8"));
        }

        new DistributedTextEditor(x, 0);
    }

    private static void removePosFile() {
        try {
            Files.delete(posFile);
        } catch (IOException ignored) {
        }
    }

    /**
     * Sets the menuButtons related to listening.
     *
     * @param online whether the new state is listening or offline
     */
    private void updateConnectionMenuButtons(boolean online) {
        Listen.setEnabled(!online);
        Connect.setEnabled(!online);
        Disconnect.setEnabled(online);
    }

    private void initializeActions() {
        Disconnect = new AbstractAction("Disconnect") {
            public void actionPerformed(ActionEvent e) {

                // Resets the listening connections
                listening = false;

                if (server != null) server.deregisterOnPort();

                try {
                    if (socket != null) socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
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
                removePosFile();
                System.exit(0);
            }
        };
        ActionMap m = area1.getActionMap();
        Copy = m.get(DefaultEditorKit.copyAction);
        Paste = m.get(DefaultEditorKit.pasteAction);

        JFrame me = this;
        Listen = new AbstractAction("Listen") {
            public void actionPerformed(ActionEvent e) {
                server = new Server(getPortNumber());
                if (!server.registerOnPort()) {
                    JOptionPane.showMessageDialog(me,
                            "Could not start listening. Port already in use.",
                            "Error starting server",
                            JOptionPane.INFORMATION_MESSAGE,
                            new ImageIcon("res/trollface.png"));
                    return;
                }

                saveOld();
                area1.setText("");
                updateConnectionMenuButtons(true);

                //sets the EventReplayer to listening mode
                updateLocalReplayer(outputDec);

                new Thread(() -> {
                    setTitle("I'm listening on " + server.getLocalHostAddress() + " on port " + getPortNumber());
                    listening = true;
                    //listen for new clients, until user "disconnects"
                    while (listening) {
                        socket = server.waitForConnectionFromClient();
                        if (socket != null) receiveEvents(socket);
                    }
                    server.deregisterOnPort();
                    goOffline();
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
                    //sets the EventReplayer to listening mode

                    updateConnectionMenuButtons(true);
                    saveOld();
                    area1.setText("");
                    updateLocalReplayer(outputDec);

                    Client client = new Client(getPortNumber());
                    socket = client.connectToServer(getIP());

                    if (socket == null) {
                        goOffline();
                        setTitle("connection failed - Disconnected");
                        return;
                    }

                    setTitle("Connected to " + getIP() + " on port " + getPortNumber());

                    receiveEvents(socket);
                    goOffline();
                }).start();

                changed = false;
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
            }
        };
    }

    private void goOffline() {
        //sets the Eventreplayer to offline mode
        updateLocalReplayer(inputDec);

        //resets the ui:
        updateConnectionMenuButtons(false);

        setTitle("Disconnected");

        emptyTextAreas();
    }

    private void updateLocalReplayer(DocumentEventCapturer dec) {
        localReplayThread.interrupt();
        EventReplayer localReplayer = new EventReplayer(dec, new LocalOutputStrategy(area2));
        localReplayThread = new Thread(localReplayer);
        localReplayThread.start();


    }

    private void receiveEvents(Socket socket) {
        Thread onlineReplayThread = new Thread(
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

        } catch (SocketException | EOFException s) {
            // SocketException is thrown when you disconnect
            // EOFException is thrown when the other disconnects
            emptyTextAreas();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        onlineReplayThread.interrupt();
    }

    /**
     * Empty the two text areas. First, the current document filter on area 1 is saved.
     * Then, it is removed, the areas are emptied, and the filter is reinstated.
     */
    private void emptyTextAreas() {
        DocumentFilter filter = ((AbstractDocument) area1.getDocument()).getDocumentFilter();

        AbstractDocument document = (AbstractDocument) area1.getDocument();
        document.setDocumentFilter(null);
        area1.setText("");
        area2.setText("");
        document.setDocumentFilter(filter);
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
        } catch (IOException ignored) {
        }
    }

}
