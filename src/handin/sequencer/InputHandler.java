package handin.sequencer;

import handin.communication.Event;
import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Queue;

public class InputHandler implements Runnable {

    private final ObjectInputStream stream;
    private final Queue<Event> eventQueue;
    private final Boolean running;

    public InputHandler(ObjectInputStream stream, Queue<Event> eventQueue) {
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
                eventQueue.add(event);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("InputHandler going down");
        }
    }
}
