package handin.text_events;

import java.io.Serializable;

public abstract class MyTextEvent implements Serializable {
    private int number;
    private int offset;

    MyTextEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public abstract int getLength();

    public abstract void setLength(int i);

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
