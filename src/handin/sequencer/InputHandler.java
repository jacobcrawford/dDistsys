package handin.sequencer;

import handin.Pair;
import handin.communication.ClientListChangeEvent;
import handin.communication.Event;
import handin.text_events.MyTextEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Queue;

class InputHandler implements Runnable {

    private final ObjectInputStream stream;
    private final Queue<Event> eventQueue;
    private final String ip;
    private final int port;

    public InputHandler(ObjectInputStream stream, Queue<Event> eventQueue, Pair<String, Integer> clientInfo) {
        this.ip = clientInfo.getFirst();
        this.port = clientInfo.getSecond();
        this.stream = stream;
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        //receive and send to Queue
        try {
            while (!Thread.interrupted()) {
                MyTextEvent event = (MyTextEvent) stream.readObject();
                eventQueue.add(event);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("InputHandler going down");
        }
        //Add the event that a new client has joined the list.
        eventQueue.add(new ClientListChangeEvent(ip, port, ClientListChangeEvent.remove));
    }
}
