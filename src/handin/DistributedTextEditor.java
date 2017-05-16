package handin;

import handin.communication.Client;
import handin.communication.Server;
import handin.sequencer.Sequencer;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class DistributedTextEditor extends JFrame implements Editor {

    private static final int SECOND_WINDOW_POSITION = 700;
    private static Path posFile;
    private boolean changed = false;

    private final JTextArea textArea;
    private final JTextField ipAddress;
    private final JTextField portNumber;
    private final JFileChooser dialog;
    private String currentFile = "Untitled";
    private Server server;
    private Action disconnect;
    private Action copy;
    private Action paste;
    private Action save;
    private Action saveAs;
    private Action listen;
    private Action connect;
    private Action quit;
    private ClientHandler clientHandler;
    private Sequencer sequencer;

    private final DocumentEventCapturer inputDec = new DocumentEventCapturer();
    private final DocumentEventCapturer outputDec = new DocumentEventCapturer();

    private DistributedTextEditor(int x) {

        textArea = new JTextArea(12, 70);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        initializeActions();

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(textArea,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1, BorderLayout.CENTER);

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

        this.setLocation(x, 0);

        file.add(listen);
        file.add(connect);
        file.add(disconnect);
        file.addSeparator();
        file.add(save);
        file.add(saveAs);
        file.add(quit);

        edit.add(copy);
        edit.add(paste);
        edit.getItem(0).setText("copy");
        edit.getItem(1).setText("paste");

        save.setEnabled(false);
        saveAs.setEnabled(false);
        disconnect.setEnabled(false);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        // Activates the "Save" and "Save as..." menus on any key press on the text area
        KeyListener k1 = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                changed = true;
                save.setEnabled(true);
                saveAs.setEnabled(true);
            }
        };
        textArea.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);
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
        new DistributedTextEditor(x);
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
        listen.setEnabled(!online);
        connect.setEnabled(!online);
        disconnect.setEnabled(online);
    }

    private void initializeActions() {
        disconnect = new AbstractAction("disconnect") {
            public void actionPerformed(ActionEvent e) {

                // Resets the listening connection

                if (server != null) server.deregisterOnPort();
                if (sequencer != null) sequencer.stop();
                clientHandler.stop();
            }
        };
        save = new AbstractAction("save") {
            public void actionPerformed(ActionEvent e) {
                if (!currentFile.equals("Untitled"))
                    saveFile(currentFile);
                else
                    saveFileAs();
            }
        };
        saveAs = new AbstractAction("save as...") {
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        };
        quit = new AbstractAction("quit") {
            public void actionPerformed(ActionEvent e) {
                saveOld();
                removePosFile();
                System.exit(0);
            }
        };
        ActionMap m = textArea.getActionMap();
        copy = m.get(DefaultEditorKit.copyAction);
        paste = m.get(DefaultEditorKit.pasteAction);

        JFrame me = this;
        listen = new AbstractAction("listen") {
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
                setTitle("I'm listening on " + server.getLocalHostAddress() + " on port " + getPortNumber());

                goOnline();

                sequencer = new Sequencer(server, textArea);
                sequencer.start();

                //start local "client"
                clientHandler = new ClientHandler();
                //8080 is the current port of the leader client.
                clientHandler.setLeaderToken(new LeaderToken(server.getLocalHostAddress(), getPortNumber()));
                System.out.println(clientHandler.start("localhost", getPortNumber(), (Editor) me, outputDec, textArea, 8080));

            }
        };

        connect = new AbstractAction("connect") {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Get the leadertoken
                setTitle("Connection...");
                LeaderToken token = getToken(getIP(), getPortNumber());
                clientHandler = new ClientHandler();
                clientHandler.setLeaderToken(token);
                clientHandler.start(token.getIp(), token.getPort(), (Editor) me, outputDec, textArea, getPortNumber());
                setTitle("Connected to " + token.getIp() + " at port " + token.getPort());
            }
        };
    }

    /**
     * @param ip   The ip of a client where the token is requested.
     * @param port The port of the client
     * @return The LeaderToken holding the information on the sequencer.
     */
    private LeaderToken getToken(String ip, int port) {
        try {
            Client client = new Client(port);
            Socket tokenSocket = client.connectToServer(ip);
            ObjectInputStream tokenGetter = new ObjectInputStream(tokenSocket.getInputStream());
            LeaderToken leaderToken = (LeaderToken) tokenGetter.readObject();
            return leaderToken;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public DocumentEventCapturer getOutDec() {
        return outputDec;
    }

    @Override
    public DocumentEventCapturer getInDec() {
        return inputDec;
    }


    /**
     * sets the editor to online mode.
     */
    public void goOnline() {
        saveOld();
        textArea.setText("");
        updateConnectionMenuButtons(true);

        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(inputDec);

        changed = false;
        save.setEnabled(false);
        saveAs.setEnabled(false);
    }

    /**
     * Returns the editor to offline mode.
     */
    public void goOffline() {
        //sets the Eventreplayer to offline mode
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(null);

        //resets the ui:
        updateConnectionMenuButtons(false);

        setTitle("Disconnected");

        emptyTextAreas();
    }

    /**
     * Empty the two text areas. First, the current document filter on area 1 is saved.
     * Then, it is removed, the areas are emptied, and the filter is reinstated.
     */
    public void emptyTextAreas() {
        DocumentFilter filter = ((AbstractDocument) textArea.getDocument()).getDocumentFilter();

        AbstractDocument document = (AbstractDocument) textArea.getDocument();
        document.setDocumentFilter(null);
        textArea.setText("");
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
            if (JOptionPane.showConfirmDialog(this, "Would you like to save " + currentFile + " ?", "save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            textArea.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            save.setEnabled(false);
        } catch (IOException ignored) {
        }
    }
}
