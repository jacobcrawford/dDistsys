package handin;

import handin.communication.Server;
import handin.output_strategy.FilterIgnoringOutputStrategy;
import handin.output_strategy.LocalOutputStrategy;
import handin.output_strategy.OutputStrategy;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
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

    private JTextArea area1;
    private JTextArea area2;
    private JTextField ipAddress;
    private JTextField portNumber;
    private JFileChooser dialog;
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
    private Coordinator coordinator;

    private DocumentEventCapturer inputDec = new DocumentEventCapturer();
    private DocumentEventCapturer outputDec = new DocumentEventCapturer();

    private Thread localReplayThread = new Thread();

    private Socket socket;
    private boolean listening;

    private DistributedTextEditor(int x, int y) {

        area1 = new JTextArea(12, 70);
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));

        initializeActions();

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
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

        this.setLocation(x, y);

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
        KeyListener k1 = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                changed = true;
                save.setEnabled(true);
                saveAs.setEnabled(true);
            }
        };
        //TODO what does this do?
        area1.addKeyListener(k1);
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

        if (arg.length > 0) {
            Settings.arbitraryDelay = Integer.parseInt(arg[0]);
        }

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
        listen.setEnabled(!online);
        connect.setEnabled(!online);
        disconnect.setEnabled(online);
    }

    private void initializeActions() {
        disconnect = new AbstractAction("disconnect") {
            public void actionPerformed(ActionEvent e) {

                // Resets the listening connection

                if (server != null) server.deregisterOnPort();
                if (coordinator!=null)coordinator.stop();
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
        ActionMap m = area1.getActionMap();
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

                coordinator = new Coordinator(server, area1);
                coordinator.start();

                //start local "client"
                clientHandler = new ClientHandler();
                System.out.println(clientHandler.start("localhost", getPortNumber(), (Editor) me));

            }
        };

        connect = new AbstractAction("connect") {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientHandler = new ClientHandler();
                setTitle(clientHandler.start(getIP(), getPortNumber(), (Editor) me));
            }
        };
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
        area1.setText("");
        updateConnectionMenuButtons(true);

        ((AbstractDocument) area1.getDocument()).setDocumentFilter(inputDec);

        //sets the EventReplayer to listening mode
        updateLocalReplayer(outputDec, new FilterIgnoringOutputStrategy(area1));

        changed = false;
        save.setEnabled(false);
        saveAs.setEnabled(false);
    }

    /**
     * Returns the editor to offline mode.
     */
    public void goOffline() {
        //sets the Eventreplayer to offline mode
        updateLocalReplayer(inputDec, new LocalOutputStrategy(area1));
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(null);

        //resets the ui:
        updateConnectionMenuButtons(false);

        setTitle("Disconnected");

        emptyTextAreas();
    }

    /**
     * Interrupts the old localreplay, and starts a new one, with the given DocumentEventCapturer.
     *
     * @param dec, the DocumentEventCapturer, which the replayer will take events from.
     */
    private void updateLocalReplayer(DocumentEventCapturer dec, OutputStrategy outputStrategy) {
        localReplayThread.interrupt();
        EventReplayer localReplayer = new EventReplayer(dec, outputStrategy);
        localReplayThread = new Thread(localReplayer);
        localReplayThread.start();
    }

    /**
     * Empty the two text areas. First, the current document filter on area 1 is saved.
     * Then, it is removed, the areas are emptied, and the filter is reinstated.
     */
    public void emptyTextAreas() {
        DocumentFilter filter = ((AbstractDocument) area1.getDocument()).getDocumentFilter();

        AbstractDocument document = (AbstractDocument) area1.getDocument();
        document.setDocumentFilter(null);
        area1.setText("");
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
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            save.setEnabled(false);
        } catch (IOException ignored) {
        }
    }

}
