package handin.sequencer;

import handin.communication.ClientListChangeEvent;
import handin.communication.Event;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;
import handin.text_events.TextRemoveEvent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class OutputHandler {

    //to remember past events.
    private final HashMap<Integer, MyTextEvent> pastTextEvents;
    private final BlockingQueue<Event> eventQueue;
    private final List<ClientBundle> outputStreams;
    private int number = 0;
    private Thread broadcastThread;

    public OutputHandler(BlockingQueue<Event> eventQueue) {
        this.outputStreams = new LinkedList<>();
        this.eventQueue = eventQueue;
        this.pastTextEvents = new HashMap<>();
        this.pastTextEvents.put(0, new TextInsertEvent(0, ""));
    }

    public void addClient(ObjectOutputStream newStream, String ip, int port) {
        outputStreams.add(new ClientBundle(port,ip,newStream));
        //Add that a new client has joined the list.
        Event event = new ClientListChangeEvent(ip,port,ClientListChangeEvent.add);
        eventQueue.add(event);
    }

    public void beginBroadcasting() {
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
                    e.printStackTrace();
                }
                broadcast(event);
            }
        });
        broadcastThread.start();
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
                            newEvent.setOffset(newEventOffset + oldEvent.getLength());
                        } else {
                            int extraEventLength = newEventEndPoint - oldEventOffset;

                            TextRemoveEvent extraEvent = new TextRemoveEvent(oldEventEndPoint, extraEventLength);
                            extraEvent.setNumber(i);
                            eventQueue.add(extraEvent);

                            newEvent.setOffset(newEventOffset);
                            newEvent.setLength(oldEventOffset - newEventOffset);
                        }
                    } else {
                        newEvent.setOffset(newEventOffset + oldEvent.getLength());
                    }
                } else if (oldEvent instanceof TextRemoveEvent) {

                    if (newEvent instanceof TextRemoveEvent) {
                        // the removeevents overlap, change length/offset, so that we dont double remove
                        if (newEventOffset >= oldEventOffset) {
                            //The oldevents begins before the new. only remove from the point that the old event stopped removing.
                            newEvent.setOffset(Math.max(newEventOffset, oldEventEndPoint) - (Math.min(newEventOffset, oldEventEndPoint) - (oldEventOffset)));
                            newEvent.setLength(newEventEndPoint - newEventOffset);
                        } else {
                            //The old revent begins later in the text, only remove until the beginning of it
                            newEvent.setLength(oldEventOffset - newEventOffset);
                            //the old event doesn't remove all the way to the end of the new event, take care of the tail.
                            if (oldEventEndPoint < newEventEndPoint) {
                                int extraEventOffSet = oldEventEndPoint - oldEvent.getLength();
                                int extraEventLength = newEventEndPoint - oldEventEndPoint;

                                TextRemoveEvent extraEvent = new TextRemoveEvent(extraEventOffSet, extraEventLength);
                                extraEvent.setNumber(i);
                                eventQueue.add(extraEvent);
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
    }

    private void broadcast(Event event) {
        //
        for (Iterator<ClientBundle> iterator = outputStreams.iterator(); iterator.hasNext(); ) {
            ClientBundle client = iterator.next();
            try {
                client.getStream().writeObject(event);
            } catch (SocketException e) {
                System.out.println("Dead OutputStream removed from OutputHandler");
                iterator.remove();
                removeClient(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeClient(ClientBundle client) {
        Event event = new ClientListChangeEvent(client.getIp(),client.getPort(),ClientListChangeEvent.remove);
        eventQueue.add(event);
    }

    public void stop() {
        broadcastThread.interrupt();
        for (ClientBundle client: outputStreams) {
            try {
                client.getStream().close();
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
