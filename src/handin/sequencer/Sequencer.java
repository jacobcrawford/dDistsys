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
    private final LinkedList<handin.Pair<String, Integer>> clientList;
    private Thread listenThread;

    public Sequencer(Server server, JTextArea textArea) {
        this.textArea = textArea;
        this.eventQueue = new LinkedBlockingDeque<>();
        this.outputHandler = new OutputHandler(eventQueue);
        this.server = server;
        clientList = new LinkedList<>();
    }

    public void start() {
        startListeningForClients();
        outputHandler.beginBroadcasting();
    }

    private void startListeningForClients() {

        // Create a new thread for listening, so it can be interrupted
        listenThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    // Block until a socket is connected
                    Socket socket = server.waitForConnectionFromClient();
                    System.out.println("new client connected");
                    // Add the outputstream to the handler
                    ObjectOutputStream outputStream = new ObjectOutputStream((socket.getOutputStream()));
                    //set the new clients textarea to match:
                    MyTextEvent initialEvent = new TextInsertEvent(0, textArea.getText());
                    initialEvent.setNumber(outputHandler.getNumber());
                    outputStream.writeObject(initialEvent);
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    //TODO fix port numbers, so that the port is 10k++
                    Pair<String,Integer> clientInfo =(handin.Pair) inputStream.readObject();
                    pushClientListOnNewClient(outputStream);
                    outputHandler.addClient(outputStream);

                    // Create an inputhandler, connect it to the outputhandler, and start its thread
                    InputHandler inputHandler = new InputHandler(inputStream, eventQueue,socket.getInetAddress().getHostAddress(),clientInfo.getSecond());
                    new Thread(inputHandler).start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        listenThread.start();
    }

    private void pushClientListOnNewClient(ObjectOutputStream outputStream) {
        for (handin.Pair<String, Integer> p : getClientList()) {
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

    public LinkedBlockingDeque<Event> getEventQueue() {
        return eventQueue;
    }

    public LinkedList<handin.Pair<String, Integer>> getClientList() {
        return clientList;
    }
}
