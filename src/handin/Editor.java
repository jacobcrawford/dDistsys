package handin;

import handin.communication.Server;

public interface Editor {
    void goOffline();

    void goOnline();

    DocumentEventCapturer getOutDec();

    DocumentEventCapturer getInDec();

    void DisplayError(String s);

    void emptyTextAreas();

    void setTitle(String newTitle);

    void startSequencer(Server server, String initialContent);

    String getText();
}
