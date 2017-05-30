package handin.output_strategy;

import handin.events.MyTextEvent;

public interface OutputStrategy {
    void output(MyTextEvent event);
}
