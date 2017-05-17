package handin.sequencer;

import java.io.ObjectOutputStream;

/**
 * Created by hjort on 5/17/17.
 */
public class ClientBundle {
    private final ObjectOutputStream stream;
    private int port;
    private String ip;

    public ClientBundle(int port, String ip, ObjectOutputStream stream) {
        this.port = port;
        this.ip = ip;
        this.stream = stream;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public ObjectOutputStream getStream() {
        return stream;
    }
}
