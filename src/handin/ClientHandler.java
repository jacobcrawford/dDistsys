package handin;

import handin.communication.Client;
import handin.output_strategy.RemoteOutputStrategy;
import handin.text_events.MyTextEvent;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler {

    public static int number = 0;
    private Socket socket;

    public String start(String ip, int port,Editor editor) {
        Client client = new Client(port);
        socket = client.connectToServer(ip);
        System.out.println("connection");
        if (socket == null) {
            return "connection failed - Disconnected";
        }

        editor.goOnline();
        new Thread(() -> {
            sendAndReceiveEvents(socket,editor);
            editor.goOffline();
        }).start();
        return "Connected to " + ip + " on port " + port;
    }

    public void stop() {
        //TODO implement this
        try{
            if (socket!=null)socket.close();
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
                new EventReplayer(inputDec, new RemoteOutputStrategy(socket))
        );
        onlineReplayThread.start();

        // Send textevents from the input stream to the outputDec
        try {
            final ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            while (socket.isConnected() && !socket.isClosed()) {
                Object o = fromClient.readObject();
                if (o instanceof MyTextEvent) {
                    MyTextEvent event = (MyTextEvent) o;
                    number = event.getNumber();
                    outputDec.addMyTextEvent(event);
                } else {
                    System.out.println("Unreadable object received");
                }
            }
            fromClient.close();

        } catch (SocketException | EOFException s) {
            // SocketException is thrown when you disconnect
            // EOFException is thrown when the other disconnects
            //s.printStackTrace();
            System.out.println("Socket/EOF-Exception cast in ClientHandler");
            editor.emptyTextAreas();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        // Stop sending from inputDec to the socket
        onlineReplayThread.interrupt();
    }

}
