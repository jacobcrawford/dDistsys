package handin.events;

import java.io.Serializable;

public abstract class MyTextEvent implements Serializable, Event {
    private int number;
    private int offset;
    private int id;

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

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void setID(int id) {
        this.id = id;
    }
}
