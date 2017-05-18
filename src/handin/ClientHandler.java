package handin;

import handin.communication.Client;
import handin.communication.ClientListChangeEvent;
import handin.communication.Server;
import handin.output_strategy.FilterIgnoringOutputStrategy;
import handin.output_strategy.OutputStrategy;
import handin.output_strategy.RemoteOutputStrategy;
import handin.text_events.MyTextEvent;

import javax.swing.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class ClientHandler {
    private final LinkedList<Pair<String, Integer>> clientList = new LinkedList<>();
    private LeaderToken leaderToken;
    private int number = 0;
    private Socket socket;
    private Thread localReplayThread = new Thread();
    private Integer actualListenPort;

    public String start(String serverIp, int serverPort, Editor editor, DocumentEventCapturer dec, JTextArea area, int listenPort) {
        Client client = new Client(serverPort);
        socket = client.connectToServer(serverIp);
        System.out.println("connection");
        if (socket == null) {
            return "connection failed - Disconnected";
        }

        editor.goOnline();
        //sets the EventReplayer to listening mode
        updateLocalReplayer(dec, new FilterIgnoringOutputStrategy(area, this));
        new Thread(() -> {
            int counter = 1;
            //Listen for Token getters.
            Socket tokenSocket;
            Server server = new Server(listenPort);
            while (!server.registerOnPort()) {
                server = new Server(listenPort + counter);
                counter++;
            }
            actualListenPort = listenPort + counter - 1;

            editor.DisplayError("Listening on port: " + (listenPort + counter - 1));
            while (socket.isConnected()) {
                tokenSocket = server.waitForConnectionFromClient();
                try {
                    ObjectOutputStream tokenSender = new ObjectOutputStream(tokenSocket.getOutputStream());
                    tokenSender.writeObject(getLeaderToken());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        // TODO: Handle interruption of this thread
        Thread communicationThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                sendAndReceiveEvents(socket, editor);
                socket = handleServerCrash();
            }
            editor.goOffline();
        });
        communicationThread.start();

        return "Connected to " + serverIp + " on port " + serverPort;
    }

    /**
     * @return A {@link Socket} to the new sequencer
     */
    private Socket handleServerCrash() {
        // Start sequencer if current client is the first in the client list
        if (isFirstInList()) startSequencer();

        // Block until the newLeaderToken is received
        LeaderToken leaderToken = receiveNewLeaderToken();

        return getSocketFromToken(leaderToken);
    }

    /**
     * Starts a sequencer
     */
    private void startSequencer() {

    }

    private LeaderToken receiveNewLeaderToken() {
        return null;
    }

    private Socket getSocketFromToken(LeaderToken leaderToken) {
        return null;
    }

    private boolean isFirstInList() {
        // TODO: Implement correctly
        return clientList.getLast().equals(new Pair<>(socket.getLocalAddress().getHostAddress(), actualListenPort));
    }

    public void stop() {
        try {
            if (socket != null) socket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Will receive events from the socket's InputStream, until the socket closes/an exception is cast.
     * In case of SocketException, or EOFException, the textFields are reset
     *
     * @param socket, the socket, which InputStream is read from.
     */
    private void sendAndReceiveEvents(Socket socket, Editor editor) {
        DocumentEventCapturer inputDec = editor.getInDec();
        DocumentEventCapturer outputDec = editor.getOutDec();
        // Create an event replayer that listens on inputDec and outputs to the socket
        Thread onlineReplayThread = new Thread(
                new EventReplayer(inputDec, new RemoteOutputStrategy(socket, this))
        );
        onlineReplayThread.start();

        // Send textevents from the input stream to the outputDec
        try {
            final ObjectInputStream fromSequencer = new ObjectInputStream(socket.getInputStream());
            while (socket.isConnected() && !socket.isClosed()) {
                Object o = fromSequencer.readObject();
                if (o instanceof MyTextEvent) {
                    MyTextEvent event = (MyTextEvent) o;
                    outputDec.addMyTextEvent(event);
                } else if (o instanceof ClientListChangeEvent) {
                    ClientListChangeEvent e = (ClientListChangeEvent) o;
                    Pair<String, Integer> client = new Pair<>(e.getIp(), e.getPort());
                    switch (e.getEvent()) {
                        case "ADD":
                            clientList.add(client);
                            System.out.println("added");
                            break;
                        case "REMOVE":
                            clientList.remove(client);
                            System.out.println("removed");
                            break;
                        default:
                            System.out.println("Bad ClientListChangeEvent received");
                    }
                } else {
                    System.out.println("Unreadable object received");
                }

            }
            fromSequencer.close();

        } catch (SocketException | EOFException s) {
            // SocketException is thrown when you disconnect
            // EOFException is thrown when the other disconnects
            System.out.println("Server disconnected");
//            editor.emptyTextAreas();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        // Stop sending from inputDec to the socket
        onlineReplayThread.interrupt();
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

    private LeaderToken getLeaderToken() {
        return leaderToken;
    }

    public void setLeaderToken(LeaderToken leaderToken) {
        this.leaderToken = leaderToken;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public LinkedList<Pair<String, Integer>> getClientList() {
        return clientList;
    }

    public Integer getListenPort() {
        return actualListenPort;
    }
}
