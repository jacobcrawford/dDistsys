package handin;

import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;
import handin.text_events.TextRemoveEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class OutputHandler {

    //to remember past events.
    private HashMap<Integer, MyTextEvent> pastTextEvents;
    private BlockingQueue<MyTextEvent> eventQueue;
    private List<ObjectOutputStream> outputStreams;
    private int number = 0;

    public OutputHandler(BlockingQueue<MyTextEvent> eventQueue) {
        this.outputStreams = new LinkedList<>();
        this.eventQueue = eventQueue;
        this.pastTextEvents = new HashMap<>();
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

                        System.out.println("received event with last change number: " + event.number + ", last change had number: " + number);
                        if (event.number < number && Settings.offsetAdjusting) {
                            System.out.println("adjusting!");
                            adjustOffset(event);
                        }
                        number++;
                        event.number = number;
                        //remember past events
                        pastTextEvents.put(event.number, event);
                        //TODO remove old events
                        //System.out.println("event received!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast(event);
                }
            }
        }).start();
    }

    /**
     * changes offset to make newEvent compatible.
     *
     * @param newEvent, the event, that is about to be inserted
     */
    private void adjustOffset(MyTextEvent newEvent) {
        for (int i = newEvent.number; i <= number; i++) {
            MyTextEvent textEvent = pastTextEvents.get(i);

            //if the previous event changes overlaps or is before this event, adjust offset.
            if (newEvent.getOffset() + newEvent.getLength() >= textEvent.getOffset()) {
                if (textEvent instanceof TextInsertEvent) {
                    newEvent.setOffset(newEvent.getOffset() + textEvent.getLength());
                } else if (textEvent instanceof TextRemoveEvent) {
                    newEvent.setOffset(newEvent.getOffset() - textEvent.getLength());
                }
            }
        }
        if (newEvent.getOffset() < 0) {
            newEvent.setOffset(0);
        }
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

    public void stop() {
        for (OutputStream out: outputStreams){
            try {
                out.close();
            } catch (IOException e) {
                System.out.println("Closing connection to client");
                e.printStackTrace();
            }
        }
    }

    public int getNumber() {
        return number;
    }
}
