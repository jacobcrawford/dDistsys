package handin;

public class MyTextEvent {
    private int offset;

    MyTextEvent(int offset) {
        this.offset = offset;
    }

    int getOffset() {
        return offset;
    }
}
