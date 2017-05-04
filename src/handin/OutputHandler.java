package handin;

import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hjort on 5/4/17.
 */
public class OutputHandler {

    private BlockingQueue<MyTextEvent> eventQueue;
    private List<ObjectOutputStream> outputStreams;

    public OutputHandler(BlockingQueue<MyTextEvent> eventQueue) {
        this.outputStreams = new LinkedList<>();
        this.eventQueue = eventQueue;
    }

    public void addClient(ObjectOutputStream newStream) {
        outputStreams.add(newStream);
    }

    public void beginBroadcasting () {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MyTextEvent event = null;
                while (true) {
                    try {
                        event = eventQueue.take();
                        System.out.println("event received!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast(event);
                }
            }
        }).start();
    }

    private void broadcast(MyTextEvent event) {
        //
        System.out.println("BroadCast!");
        for (ObjectOutputStream outputStream: outputStreams) {
            try {
                outputStream.writeObject(event);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO Handle crash
            }
        }
    }
}
