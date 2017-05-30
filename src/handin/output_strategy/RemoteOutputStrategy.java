package handin.output_strategy;

import handin.ClientHandler;
import handin.Configuration;
import handin.Pair;
import handin.events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class RemoteOutputStrategy implements OutputStrategy {
    private final ObjectOutputStream out;
    private final Socket socket;
    private final ClientHandler clientHandler;

    public RemoteOutputStrategy(Socket socket, ClientHandler clientHandler, Semaphore semaphore) {
        this.socket = socket;
        this.clientHandler = clientHandler;
        out = createOutputStream();
        try {
            //Write client info to the sequencer
            System.out.println("ACQUIREING SMP");
            semaphore.acquire();
            System.out.println("DONE ACQUIREING SMP");
            int port = clientHandler.getListenPort();
            if (out != null)
                out.writeObject(new Pair<>(Configuration.getIP(), port));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ObjectOutputStream createOutputStream() {
        try {
            return new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void output(MyTextEvent event) {
        if (socket.isConnected()) {
            event.setNumber(clientHandler.getNumber());
            //System.out.println("sending event! with number: " + event.getNumber());
            try {
                out.writeObject(event);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("socket not connected!");
        }
    }
}
