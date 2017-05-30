package handin.sequencer;

import handin.Pair;
import handin.communication.ClientListChangeEvent;
import handin.communication.Event;
import handin.communication.Server;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;

public class Sequencer {
    private final LinkedBlockingDeque<Event> eventQueue;
    private final OutputHandler outputHandler;
    private final Server server;
    private final JTextArea textArea;
    private final LinkedList<Pair<String, Integer>> clientList;
    private Thread listenThread;

    public Sequencer(Server server, String initialContent) {
        this.textArea = new JTextArea(initialContent);
        this.eventQueue = new LinkedBlockingDeque<>();
        this.outputHandler = new OutputHandler(eventQueue, this);
        this.server = server;
        clientList = new LinkedList<>();
    }

    public void start() {
        startListeningForClients();
        outputHandler.beginBroadcasting(textArea);
    }

    private void startListeningForClients() {

        // Create a new thread for listening, so it can be interrupted
        listenThread = new Thread(() -> {
            while (!Thread.interrupted()) try {
                // Block until a socket is connected
                Socket socket = server.waitForConnectionFromClient();
                System.out.println("new client connected");
                // Add the outputstream to the handler
                ObjectOutputStream outputStream;
                if (socket != null) {
                    outputStream = new ObjectOutputStream((socket.getOutputStream()));
                } else {
                    return;
                }

                // Tell the outputHandler that a new client is connecting, so it stops broadcasting temporarily
                outputHandler.setNewClientIsConnecting(true);

                //set the new clients textarea to match:
                MyTextEvent initialEvent = new TextInsertEvent(0, textArea.getText());
                initialEvent.setNumber(outputHandler.getNumber());

                outputStream.writeObject(initialEvent);
                System.out.println("Wrote initial event " + textArea.getText() + " to new client");
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                Pair<String, Integer> clientInfo = verifyClientInfoObject(inputStream.readObject());
                if (clientInfo == null) {
                    System.out.println("BAD CLIENT INFO OBJECT RECEIVED");
                    outputHandler.setNewClientIsConnecting(false);
                    return;
                }

                pushClientListOnNewClient(outputStream);
                //Add client after the push so that it is not added twice
                clientList.add(clientInfo);
                //Add the ADDEvent to the queue
                Event event = new ClientListChangeEvent(clientInfo.getFirst(), clientInfo.getSecond(), ClientListChangeEvent.add);
                outputHandler.addClient(outputStream);
                eventQueue.add(event);

                // Create an inputhandler, connect it to the outputhandler, and start its thread
                InputHandler inputHandler = new InputHandler(inputStream, eventQueue, clientInfo);
                new Thread(inputHandler).start();

                // Tell the outputHandler that a new client is done connecting, so it resumes broadcasting
                outputHandler.setNewClientIsConnecting(false);
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
        listenThread.start();
    }

    /**
     * Checks if the received object is actually a valid client info {@link Pair}
     *
     * @param receivedObject The object to check
     * @return A valid client info {@link Pair}, or null if the object is invalid
     */
    private Pair<String, Integer> verifyClientInfoObject(Object receivedObject) {
        if (receivedObject instanceof Pair) {
            Object first = ((Pair) receivedObject).getFirst();
            Object second = ((Pair) receivedObject).getSecond();
            if (first instanceof String && second instanceof Integer) {
                return new Pair<>((String) first, (Integer) second);
            }
        }
        return null;
    }

    private void pushClientListOnNewClient(ObjectOutputStream outputStream) {
        for (Pair<String, Integer> p : getClientList()) {
            try {
                outputStream.writeObject(new ClientListChangeEvent(p.getFirst(), p.getSecond(), "ADD"));
                System.out.println("PUSH CLIENT TO NEW CLIENT:" + p.getFirst() + " " + p.getSecond());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void stop() {
        outputHandler.stop();
        server.deregisterOnPort();
        listenThread.interrupt();
    }

    private LinkedList<Pair<String, Integer>> getClientList() {
        return clientList;
    }

    public void removeClient(ClientListChangeEvent changeEvent) {
        synchronized (clientList) {
            clientList.remove(new Pair<>(changeEvent.getIp(), changeEvent.getPort()));
        }
    }
}
