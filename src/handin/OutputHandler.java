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
        this.pastTextEvents.put(0, new TextInsertEvent(0, ""));
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

                        System.out.println("received event with last change number: " + event.getNumber() + ", last change had number: " + number);
                        if (event.getNumber() < number && Settings.offsetAdjusting) {
                            System.out.println("adjusting!");
                            adjustOffset(event);
                        }
                        number++;
                        event.setNumber(number);
                        //remember past events
                        System.out.println("saving: " + event + " as number " + event.getNumber());
                        pastTextEvents.put(event.getNumber(), event);
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
        System.out.println("start: " + newEvent);
        for (int i = newEvent.getNumber() + 1; i <= number; i++) {
            MyTextEvent oldEvent = pastTextEvents.get(i);
            System.out.println(i + ": " + oldEvent);

            int newEventOffset = newEvent.getOffset();
            int newEventEndPoint = newEvent.getOffset() + newEvent.getLength();
            int oldEventOffset = oldEvent.getOffset();
            int oldEventEndPoint = oldEvent.getOffset() + oldEvent.getLength();

            //if the previous event changes overlaps or is before this event, adjust offset.
            if (newEventEndPoint >= oldEventOffset) {
                if (oldEvent instanceof TextInsertEvent) {
                    if (newEvent instanceof TextRemoveEvent) {
                        if (newEventOffset >= oldEventOffset) {
                            newEvent.setOffset(newEventOffset + oldEvent.getLength());
                        } else {
//                            // Delete from oldEventOffset to the newEventOffset
//                            int lengthBeforeInsertion = oldEventOffset - newEventOffset;
//                            int offsetBeforeInsertion = oldEventOffset;
//                            // Also delete from the end of the insertion to the rest
//                            int lengthAfterInsertion = newEvent.getLength() - lengthBeforeInsertion;
//                            int offsetAfterInsertion = offsetBeforeInsertion + lengthBeforeInsertion + oldEvent.getLength();
//
//                            TextRemoveEvent extraEvent = new TextRemoveEvent(offsetAfterInsertion, lengthAfterInsertion);
//                            extraEvent.setNumber(i);
//                            eventQueue.add(extraEvent);
//
//                            System.out.println("OldEventOffset: " + offsetBeforeInsertion);
//                            newEvent.setOffset(offsetBeforeInsertion);
//                            newEvent.setLength(lengthBeforeInsertion);


                            // oldEvent = insert(bc)
                            // newEvent = delete(abcd)

                            // oldEventOffset > newEventOffset
                            // slet fra newEventOffset til oldEventOffset (længde X)
                            // og slet fra oldEventOffset + length(oldEvent) til length(newEvent)-X
                            int extraEventOffSet = oldEventOffset + oldEvent.getLength() - (oldEventOffset - newEventOffset);
                            int extraEventLength = newEvent.getLength() - (oldEventOffset - newEventOffset);
                            TextRemoveEvent extraEvent = new TextRemoveEvent(extraEventOffSet, extraEventLength);
                            extraEvent.setNumber(i);
                            eventQueue.add(extraEvent);

                            newEvent.setOffset(newEventOffset);
                            newEvent.setLength(oldEventOffset - newEventOffset);

                            System.out.println("New event removing " + newEvent.getLength() + " from " + newEvent.getOffset());
                            System.out.println("Extra event removing " + extraEvent.getLength() + " from " + extraEvent.getOffset());
                        }
                    } else {
                        newEvent.setOffset(newEventOffset + oldEvent.getLength());
                    }
                } else if (oldEvent instanceof TextRemoveEvent) {

                    if (newEvent instanceof TextRemoveEvent) {
                        // the removeevents overlap, change length/offset, so that we dont double remove
                        if (newEventOffset >= oldEventOffset) {
                            //The oldevents begins before the new. only remove from the point that the old event stopped removing.
                            newEvent.setOffset(Math.max(newEventOffset, oldEventEndPoint));
                            newEvent.setLength(newEventEndPoint - newEvent.getOffset());
                        } else {
                            //The old revent begins later in the text, only remove until the beginning of it
                            newEvent.setLength(oldEventOffset - newEventOffset);
                            //the old event doesn't remove all the way to the end of the new event, take care of the tail.
                            if (oldEventEndPoint < newEventEndPoint) {
                                //TODO handle if the old remove even splits the new in two parts
                            }
                        }
                    } else {
                        newEvent.setOffset(newEventOffset - oldEvent.getLength());
                    }
                }
            }
            if (newEvent.getOffset() < 0) {
                newEvent.setOffset(0);
            } else if (newEvent.getLength() < 0) {
                newEvent.setLength(0);
            }
        }
        System.out.println("end: " + newEvent);
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
