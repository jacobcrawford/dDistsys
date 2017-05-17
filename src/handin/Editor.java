package handin;

public interface Editor {
    void goOffline();

    void goOnline();

    DocumentEventCapturer getOutDec();

    DocumentEventCapturer getInDec();

    void DisplayError(String s);

    void emptyTextAreas();
}
