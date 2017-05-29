package handin.sequencer;

import handin.communication.ClientListChangeEvent;
import handin.communication.Event;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;
import handin.text_events.TextRemoveEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class OutputHandler {

    private final HashMap<Integer, MyTextEvent> pastTextEvents;
    private final BlockingQueue<Event> eventQueue;
    private final List<ObjectOutputStream> outputStreams;
    private int number = 0;
    private Thread broadcastThread;
    private Sequencer sequencer;

    public OutputHandler(BlockingQueue<Event> eventQueue, Sequencer sequencer) {
        this.sequencer = sequencer;
        this.outputStreams = new LinkedList<>();
        this.eventQueue = eventQueue;
        this.pastTextEvents = new HashMap<>();
        this.pastTextEvents.put(0, new TextInsertEvent(0, ""));
    }

    public synchronized void addClient(ObjectOutputStream newStream) {
        outputStreams.add(newStream);
    }

    public void beginBroadcasting(JTextArea sharedArea) {
        broadcastThread = new Thread(() -> {
            Event event = null;
            while (!Thread.interrupted()) {
                try {
                    event = eventQueue.take();
                    if (event instanceof MyTextEvent) {
                        MyTextEvent textEvent = (MyTextEvent) event;
                        if (textEvent.getNumber() < number) {
                            adjustOffset(textEvent);
                        }
                        number++;
                        textEvent.setNumber(number);
                        //remember past events
                        pastTextEvents.put(textEvent.getNumber(), textEvent);
                    }

                    //TODO remove old events
                } catch (InterruptedException e) {
                    System.out.println("OutputHandler stopped");
                }
                outputToArea(sharedArea,event);
                broadcast(event);
            }
        });
        broadcastThread.start();
    }

    private void outputToArea(JTextArea sharedArea, Event event) {
        if (event instanceof TextInsertEvent){
            sharedArea.insert(((TextInsertEvent)event).getText(),((TextInsertEvent) event).getOffset());
        }else if(event instanceof TextRemoveEvent){
            System.out.println(event);
            sharedArea.replaceRange(null, ((TextRemoveEvent) event).getOffset(), ((TextRemoveEvent) event).getOffset()
                    + (((TextRemoveEvent) event)).getLength());
        } else if (event instanceof ClientListChangeEvent) {
            ClientListChangeEvent changeEvent = (ClientListChangeEvent) event;
            if (changeEvent.getEvent().equals(ClientListChangeEvent.remove)) {
                sequencer.removeClient(changeEvent);
            }
        }
    }

    /**
     * changes offset to make newEvent compatible.
     *
     * @param newEvent, the event, that is about to be inserted
     */
    private void adjustOffset(MyTextEvent newEvent) {
        for (int i = newEvent.getNumber() + 1; i <= number; i++) {
            MyTextEvent oldEvent = pastTextEvents.get(i);
            int newEventOffset = newEvent.getOffset();
            int newEventEndPoint = newEvent.getOffset() + newEvent.getLength();
            int oldEventOffset = oldEvent.getOffset();
            int oldEventEndPoint = oldEvent.getOffset() + oldEvent.getLength();

            //if the previous event changes overlaps or is before this event, adjust offset.
            if (newEventEndPoint >= oldEventOffset) {
                if (oldEvent instanceof TextInsertEvent) {
                    if (newEvent instanceof TextRemoveEvent) {
                        if (newEventOffset >= oldEventOffset) {
                            System.out.println(newEvent + "," + oldEvent + ": Case 6");
                            newEvent.setOffset(newEventOffset + oldEvent.getLength());
                        } else {
                            System.out.println(newEvent + "," + oldEvent + ": Case 5");
                            int extraEventLength = newEventEndPoint - oldEventOffset;

                            TextRemoveEvent extraEvent = new TextRemoveEvent(oldEventEndPoint, extraEventLength);
                            extraEvent.setNumber(i);
                            eventQueue.add(extraEvent);

                            newEvent.setOffset(newEventOffset);
                            newEvent.setLength(oldEventOffset - newEventOffset);
                        }
                    } else {
                        System.out.println(newEvent + "," + oldEvent + ": Case 4");
                        newEvent.setOffset(newEventOffset + oldEvent.getLength());
                    }
                } else if (oldEvent instanceof TextRemoveEvent) {

                    if (newEvent instanceof TextRemoveEvent) {
                        // the removeevents overlap, change length/offset, so that we dont double remove
                        if (newEventOffset >= oldEventOffset) {
                            //The oldevents begins before the new. only remove from the point that the old event stopped removing.
                            System.out.println(newEvent + "," + oldEvent + ": Case 1");
                            if (newEventOffset >= oldEventEndPoint) {
                                newEvent.setOffset(newEventOffset-oldEvent.getLength());
                            } else {
                                newEvent.setOffset(oldEventEndPoint - oldEvent.getLength());
                                newEvent.setLength(newEventEndPoint-oldEventEndPoint);
                            }
                        } else {
                            //The old revent begins later in the text, only remove until the beginning of it
                            System.out.println(newEvent + "," + oldEvent + ": Case 2");
                            newEvent.setLength(oldEventOffset - newEventOffset);
                            //the old event doesn't remove all the way to the end of the new event, take care of the tail.
                            if (oldEventEndPoint < newEventEndPoint) {
                                // Implicit that extraEventOffSet = oldEventOffset;
                                int extraEventLength = newEventEndPoint - oldEventEndPoint;

                                TextRemoveEvent extraEvent = new TextRemoveEvent(oldEventOffset, extraEventLength);
                                extraEvent.setNumber(i);
                                eventQueue.add(extraEvent);
                            }
                        }
                    } else {
                        System.out.println(newEvent + "," + oldEvent + ": Case 3");
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
    }

    private synchronized void broadcast(Event event) {
        //
        synchronized (outputStreams) {
        for (Iterator<ObjectOutputStream> iterator = outputStreams.iterator(); iterator.hasNext(); ) {
            ObjectOutputStream stream = iterator.next();
            try {
                stream.writeObject(event);
            } catch (SocketException e) {
                System.out.println("Dead OutputStream removed from OutputHandler");
                iterator.remove();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }}


    public synchronized void stop() {
        broadcastThread.interrupt();
        for (ObjectOutputStream stream: outputStreams) {
            try {
                stream.close();
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
