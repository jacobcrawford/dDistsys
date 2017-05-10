package handin;

import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Queue;


public class InputHandler implements Runnable {

    private ObjectInputStream stream;
    private Queue<MyTextEvent> eventQueue;
    private Boolean running;

    public InputHandler(ObjectInputStream stream, Queue<MyTextEvent> eventQueue) {
        running=true;
        this.stream = stream;
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        //receive and send to Queue
            try {
                while(running) {
                    MyTextEvent event = (MyTextEvent) stream.readObject();
                    System.out.println("event received!");
                    eventQueue.add(event);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

    }
}
