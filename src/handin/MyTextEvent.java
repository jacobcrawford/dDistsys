package handin;

import java.io.Serializable;

public class MyTextEvent implements Serializable {
    private int offset;

    MyTextEvent(int offset) {
        this.offset = offset;
    }

    int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
