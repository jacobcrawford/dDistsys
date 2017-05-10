package handin;

import handin.text_events.MyTextEvent;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Queue;

public class InputHandler implements Runnable {

    private final ObjectInputStream stream;
    private final Queue<MyTextEvent> eventQueue;
    private final Boolean running;

    public InputHandler(ObjectInputStream stream, Queue<MyTextEvent> eventQueue) {
        running = true;
        this.stream = stream;
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        //receive and send to Queue
        try {
            while (running) {
                MyTextEvent event = (MyTextEvent) stream.readObject();
                System.out.println("event received!");
                eventQueue.add(event);
            }
        } catch (EOFException e) {
            System.out.println("InputHandler going down");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
