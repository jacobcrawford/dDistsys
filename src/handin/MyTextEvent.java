package handin;

public class MyTextEvent {
	MyTextEvent(int offset) {
		this.offset = offset;
	}
	private int offset;
	int getOffset() { return offset; }
}
