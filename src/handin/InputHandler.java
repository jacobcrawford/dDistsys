package handin;

import handin.text_events.MyTextEvent;

import java.io.ObjectInputStream;
import java.util.Queue;

/**
 * Created by hjort on 5/4/17.
 */
public class InputHandler implements Runnable {

    public InputHandler(ObjectInputStream stream, Queue<MyTextEvent> EventQueue) {

    }

    @Override
    public void run() {
        //receive and send to Queue
    }
}
