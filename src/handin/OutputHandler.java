package handin;

import handin.text_events.MyTextEvent;

import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hjort on 5/4/17.
 */
public class OutputHandler {

    private BlockingQueue<MyTextEvent> eventQueue;

    public OutputHandler(BlockingQueue<MyTextEvent> eventQueue) {

        this.eventQueue = eventQueue;
    }

    public void addClient() {

    }

    public void beginBroadcasting () {
        //start thread
        MyTextEvent event = null;
        try {
            event = eventQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        broadcast(event);
    }

    private void broadcast(MyTextEvent event) {

    }
}
