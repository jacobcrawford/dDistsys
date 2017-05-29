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
import java.util.List;
import java.util.concurrent.Semaphore;

import static handin.Configuration.getIP;
import static handin.Configuration.serverPort;

public class ClientHandler {
    private LinkedList<Pair<String, Integer>> clientList = new LinkedList<>();
    private LeaderToken leaderToken;
    private int number = 0;
    private Socket socket;
    private Thread localReplayThread = new Thread();
    private Editor editor;
    private TokenThreadHandler tokenThreadHandler;
    private Semaphore semaphore;

    public String start(String serverIp, int serverPort, Editor editor, DocumentEventCapturer dec, JTextArea area) {
        Client client = new Client(serverPort);
        this.editor = editor;
        semaphore = new Semaphore(0);
        socket = client.connectToServer(serverIp);
        System.out.println("connection");
        if (socket == null) {
            return "connection failed - Disconnected";
        }

        editor.goOnline();
        //sets the EventReplayer to listening mode
        tokenThreadHandler = new TokenThreadHandler(editor, getLeaderToken(),semaphore);

        updateLocalReplayer(dec, new FilterIgnoringOutputStrategy(area, this));
        new Thread(tokenThreadHandler).start();
        // TODO: Handle interruption of this thread
        Thread communicationThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                sendAndReceiveEvents(socket, editor);
                socket = handleServerCrash(editor.getText());
                editor.emptyTextAreas();
            }
            editor.goOffline();
        });
        communicationThread.start();

        return "Connected to " + serverIp + " on port " + serverPort;
    }

    /**
     * Handles whenever a sequencer goes down
     *
     * @param initialContent The initial content of the new sequencer
     * @return A {@link Socket} to the new sequencer
     */
    private Socket handleServerCrash(String initialContent) {
        //allow remoteOutStrategy to send port once more.
        semaphore.release();
        System.out.println("Print clients");
        for (Pair p : clientList) System.out.println(p.getFirst()+" "+p.getSecond());
        // Remove the now invalid leader token
        tokenThreadHandler.resetLeaderToken();

        System.out.println("Current List is: ");
        for (Pair<String, Integer> stringIntegerPair : clientList) {
            System.out.println(stringIntegerPair);
        }
        System.out.println("--------------");


        LeaderToken leaderToken = null;

        while (leaderToken == null) {
            Pair<String, Integer> elected = clientList.getLast();

            if (weAreNewSequencer(elected)) {
                System.out.println(elected + " is the new sequencer starting a sequencer thread");
                new Thread(() -> startSequencer(initialContent, clientList)).start();
            }

            while (isAlive(elected) && leaderToken==null) {
                leaderToken = receiveNewLeaderToken();
            }
            if (leaderToken==null) clientList.removeLast();
        }

//        int currentSequencerIndex = 1; // This might be zero in second round of crashing
//        while (leaderToken == null) {
//            if (weAreNewSequencer(currentSequencerIndex)) new Thread(() -> startSequencer(initialContent, clientList)).start();
//            leaderToken = receiveNewLeaderToken();
//            currentSequencerIndex++;
//        }

        clientList = new LinkedList<>();

        System.out.println(leaderToken);
        // Return the socket opened using the leaderToken
        return getSocketFromToken(leaderToken);
    }

    private boolean isAlive(Pair<String, Integer> elected) {
        if (weAreNewSequencer(elected)) return true;
        else {
            Client client = new Client(elected.getSecond());
            Socket socket = client.connectToServer(elected.getFirst());
            if (socket != null) {
                System.out.println("Socket is not null");
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                return true;
            } else {
                System.out.println("Socket is null");
                return false;
            }
        }
    }

    private boolean weAreNewSequencer(Pair<String, Integer> elected) {
        System.out.println("Elected: " + elected);
        System.out.println("Me: " + new Pair<>(getIP(), getListenPort()));

        Pair me = new Pair<>(getIP(), getListenPort());
        return elected.equals(me);
    }

    /**
     * Starts a sequencer
     */
    private void startSequencer(String initialContent, List<Pair<String, Integer>> clientList) {
        System.out.println("im the new sequencer");
        // Start the new server, register it on the port and update the local title
        Server server = new Server(serverPort);
        server.registerOnPort();
        String hostAddress = server.getLocalHostAddress();
        editor.setTitle("I'm listening on " + hostAddress + " on port " + serverPort);

        editor.startSequencer(server, initialContent);

        // Send out a new leader token to everyone on the client list (including yourself)
        for (Pair<String, Integer> client : clientList) {
            System.out.println("Sending new leader token to " + client);
            LeaderToken leaderToken = new LeaderToken(hostAddress, serverPort);
            send(leaderToken, client);
            System.out.println("Sent new leader token " + leaderToken);
        }
    }

    private void send(LeaderToken leaderToken, Pair<String, Integer> receiverInfo) {
        Client client = new Client(receiverInfo.getSecond());
        Socket socket;

        socket = client.connectToServer(receiverInfo.getFirst());
        if (socket == null) {
            System.out.println("Error sending new leader token to client " + receiverInfo + " failed");
            return;
        }

        try {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

            // Read the leader token from client
            inputStream.readObject();
            // Write the new leader token to client
            outputStream.writeObject(leaderToken);
        } catch (IOException e) {
            System.out.println("Error writing to client " + receiverInfo.getFirst() + ":" + receiverInfo.getSecond());
        } catch (ClassNotFoundException e) {
            System.out.println("Received unreadable object from client");
        }
    }

    private LeaderToken receiveNewLeaderToken() {
        LeaderToken leaderToken = null;
        while (leaderToken == null) {
            leaderToken = tokenThreadHandler.getLeaderToken();
            System.out.println("Receiving leadertoken");
            sleep(10);
        }
        return leaderToken;

//        for (int i = 1; i < connectionAttemptsToNewSequencer; i++) {
//            LeaderToken result = tokenThreadHandler.getLeaderToken();
//            if (result != null) return result;
//            sleep(waitPerConnectionAttempt);
//        }
//        return null;
    }

    private void sleep(@SuppressWarnings("SameParameterValue") int timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
        } catch (InterruptedException ignored) {
        }
    }

    private Socket getSocketFromToken(LeaderToken leaderToken) {
        Client client = new Client(leaderToken.getPort());
        return client.connectToServer(leaderToken.getIp());
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
                new EventReplayer(inputDec, new RemoteOutputStrategy(socket, this,semaphore))
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
                            System.out.println("added " + client.getFirst() + " " + client.getSecond());
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

    public Integer getListenPort() {
        return tokenThreadHandler.getListenPort();
    }
}
