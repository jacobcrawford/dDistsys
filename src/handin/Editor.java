package handin;

public interface Editor {
    void goOffline();

    void goOnline();

    DocumentEventCapturer getOutDec();

    DocumentEventCapturer getInDec();

    void emptyTextAreas();
}
