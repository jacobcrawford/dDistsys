package handin;

/**
 * Created by hjort on 5/4/17.
 */
public interface Editor {
    void goOffline();

    void goOnline();

    DocumentEventCapturer getOutDec();

    DocumentEventCapturer getInDec();

    void emptyTextAreas();
}
