package handin.output_strategy;


import handin.ClientHandler;
import handin.Settings;
import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class RemoteOutputStrategy implements OutputStrategy {
    private final ObjectOutputStream out;
    private Socket socket;

    public RemoteOutputStrategy(Socket socket) {
        this.socket = socket;
        out = createOutputStream();
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
            event.setNumber(ClientHandler.number);
            //TODO find better way to track number

            System.out.println("sending event! with number: " + event.getNumber());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Settings.arbitraryDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.writeObject(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } else {
            System.out.println("socket not connected!");
        }

    }
}