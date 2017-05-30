package handin.events;

public class TextInsertEvent extends MyTextEvent {

    private String text;

    public TextInsertEvent(int offset, String text) {
        super(offset);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public int getLength() {
        return text.length();
    }

    @Override
    public void setLength(int i) {
        if (i == 0) {
            text = "";
            System.out.println("this shouldn't happen!");
        } else {
            throw new UnsupportedOperationException("cant change length of insert event");
        }
    }

    @Override
    public String toString() {
        return "{insert " + text + " at " + getOffset() + "}";
    }
}

