package handin;

import handin.communication.Server;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;

public class Sequencer {
    private LinkedBlockingDeque<MyTextEvent> eventQueue;
    private OutputHandler outputHandler;
    private Thread listenThread;
    private Server server;
    private JTextArea textArea;

    public Sequencer(Server server, JTextArea textArea) {
        this.textArea = textArea;
        this.eventQueue = new LinkedBlockingDeque<>();
        this.outputHandler = new OutputHandler(eventQueue);
        this.server = server;
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
                    initialEvent.number = outputHandler.getNumber();
                    outputStream.writeObject(initialEvent);
                    outputHandler.addClient(outputStream);

                    // Create an inputhandler, connect it to the outputhandler, and start its thread
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    InputHandler inputHandler = new InputHandler(inputStream, eventQueue);
                    new Thread(inputHandler).start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        listenThread.start();
    }

    public void stop() {
        outputHandler.stop();
        listenThread.interrupt();
    }
}
