package handin;

import handin.communication.Server;
import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;

public class Coordinator {
    private LinkedBlockingDeque<MyTextEvent> eventQueue;
    private OutputHandler outputHandler;
    private Thread listenThread;
    private Server server;

    public Coordinator(Server server) {
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

                    // Add the outputstream to the handler
                    ObjectOutputStream outputStream = new ObjectOutputStream((socket.getOutputStream()));
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
    }

    public void stop() {
        listenThread.interrupt();
    }
}
