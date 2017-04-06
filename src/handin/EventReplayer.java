package handin;

import javax.swing.*;
import java.awt.*;

/**
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 */
public class EventReplayer implements Runnable {

    private DocumentEventCapturer dec;
    private OutputStrategy outputStrategy;

    public EventReplayer(DocumentEventCapturer dec, OutputStrategy outputStrategy) {
        this.dec = dec;
        this.outputStrategy = outputStrategy;
    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                final MyTextEvent tie = dec.take();
                outputStrategy.output(tie);
            } catch (InterruptedException ex) {
                wasInterrupted = true;
            }
        }
        System.out.println("I'm the thread running the EventReplayer, now I die!");
    }

    public void waitForOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException _) {
        }
    }
}
