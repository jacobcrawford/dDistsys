package handin.sequencer;

import handin.Pair;
import handin.events.ClientListChangeEvent;
import handin.events.Event;
import handin.events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Queue;

class InputHandler implements Runnable {

    private final ObjectInputStream stream;
    private final Queue<Event> eventQueue;
    private final String ip;
    private final int port;
    private static int idCount;
    private int id;

    public InputHandler(ObjectInputStream stream, Queue<Event> eventQueue, Pair<String, Integer> clientInfo) {
        this.ip = clientInfo.getFirst();
        this.port = clientInfo.getSecond();
        this.stream = stream;
        this.eventQueue = eventQueue;
        this.id = idCount++;
    }

    @Override
    public void run() {
        //receive and send to Queue
        try {
            while (!Thread.interrupted()) {
                MyTextEvent event = (MyTextEvent) stream.readObject();
                event.setID(id);
                eventQueue.add(event);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("InputHandler going down");
        }
        //Add the event that a new client has joined the list.
        Event lastEvent = new ClientListChangeEvent(ip,port,ClientListChangeEvent.remove);
        lastEvent.setID(id);
        eventQueue.add(lastEvent);
    }

    public int getId() {
    return id;
    }
}
