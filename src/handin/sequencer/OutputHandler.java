package handin.sequencer;

import handin.events.ClientListChangeEvent;
import handin.events.Event;
import handin.events.MyTextEvent;
import handin.events.TextInsertEvent;
import handin.events.TextRemoveEvent;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.BlockingQueue;

class OutputHandler {

    private final HashMap<Integer, MyTextEvent> pastTextEvents;
    private final BlockingQueue<Event> eventQueue;
    private final List<ObjectOutputStream> outputStreams;
    private int number = 0;
    private Thread broadcastThread;
    private final Sequencer sequencer;
    private final HashMap<Integer, Integer> clientVersion;
    private boolean newClientIsConnecting;

    public OutputHandler(BlockingQueue<Event> eventQueue, Sequencer sequencer) {
        this.sequencer = sequencer;
        this.outputStreams = new LinkedList<>();
        this.eventQueue = eventQueue;
        this.pastTextEvents = new HashMap<>();
        this.pastTextEvents.put(0, new TextInsertEvent(0, ""));
        clientVersion = new HashMap<>();
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
                    cleanHistory(event);
                    if (event instanceof MyTextEvent) {
                        MyTextEvent textEvent = (MyTextEvent) event;
                        if (textEvent.getNumber() < number) {
                            adjustOffset(textEvent);
                        }
                        number++;
                        textEvent.setNumber(number);
                        //remember past events..
                        pastTextEvents.put(textEvent.getNumber(), textEvent);
                    }
                } catch (InterruptedException e) {
                    System.out.println("OutputHandler stopped");
                }
                outputToArea(sharedArea, event);
                broadcast(event);
            }
        });
        broadcastThread.start();
    }

    private void cleanHistory(Event newEvent) {
        if (newEvent instanceof TextInsertEvent || newEvent instanceof TextRemoveEvent) {
            MyTextEvent event = (MyTextEvent) newEvent;
            clientVersion.put(event.getID(),event.getNumber());
            cleanup();
        } else if (newEvent instanceof ClientListChangeEvent) {
            ClientListChangeEvent event = (ClientListChangeEvent) newEvent;
            if (event.getEvent().equals(ClientListChangeEvent.remove)){
                clientVersion.remove(event.getID());
                cleanup();
            } else if (event.getEvent().equals(ClientListChangeEvent.add)) {
                clientVersion.put(event.getID(),-1);
            }
        }
    }

    private void cleanup() {
        //get the minimum version
        int minimum = Integer.MAX_VALUE;
        for (Integer i: clientVersion.values()) {
            if (i>=0) {
                minimum = Math.min(i,minimum);
            }
        }
        //the actual cleanup
        Iterator<Integer> iterator = pastTextEvents.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next()<minimum) {
                iterator.remove();
            }
        }
    }

    private void outputToArea(JTextArea sharedArea, Event event) {
        if (event instanceof TextInsertEvent) {
            sharedArea.insert(((TextInsertEvent) event).getText(), ((TextInsertEvent) event).getOffset());
        } else if (event instanceof TextRemoveEvent) {
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
                        // the remove events overlap, change length/offset, so that we don't double remove
                        if (newEventOffset >= oldEventOffset) {
                            //The old events begins before the new. only remove from the point that the old event stopped removing.
                            if (newEventOffset >= oldEventEndPoint) {
                                newEvent.setOffset(newEventOffset - oldEvent.getLength());
                            } else {
                                newEvent.setOffset(oldEventEndPoint - oldEvent.getLength());
                                newEvent.setLength(newEventEndPoint - oldEventEndPoint);
                            }
                        } else {
                            //The old event begins later in the text, only remove until the beginning of it
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
        while (newClientIsConnecting) sleep();

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
        }
    }

    private void sleep() {
        try {
            Thread.sleep(handin.Configuration.waitForNewClientToConnectTimeout);
        } catch (InterruptedException ignored) {
        }
    }

    public synchronized void stop() {
        broadcastThread.interrupt();
        synchronized (outputStreams) {
            for (ObjectOutputStream stream : outputStreams) {
                try {
                    stream.close();
                } catch (IOException e) {
                    System.out.println("Closing connection to client");
                    e.printStackTrace();
                }
            }
        }
    }

    public int getNumber() {
        return number;
    }

    public void setNewClientIsConnecting(boolean newClientIsConnecting) {
        this.newClientIsConnecting = newClientIsConnecting;
    }
}
