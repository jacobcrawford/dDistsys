package handin.events;

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

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "{remove " + getLength() + " starting from " + getOffset() + "}";
    }
}
