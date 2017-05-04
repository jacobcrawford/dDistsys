package handin;

import handin.output_strategy.OutputStrategy;
import handin.text_events.MyTextEvent;

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
                ex.printStackTrace();
                wasInterrupted = true;
            }
        }
    }
}
