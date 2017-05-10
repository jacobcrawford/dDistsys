package handin.text_events;

public class TextRemoveEvent extends MyTextEvent {

    private int length;

    public TextRemoveEvent(int offset, int length) {
        super(offset);
        this.length = length;
    }

    @Override
    public int getLength() {
        return length;
    }
}
