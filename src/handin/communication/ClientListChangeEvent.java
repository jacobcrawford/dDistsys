package handin.communication;

import java.io.Serializable;

public class ClientListChangeEvent implements Event, Serializable {
    public static final String remove = "REMOVE";
    public static final String add = "ADD";
    private final String ip;
    private final int port;
    private final String event;

    public ClientListChangeEvent(String ip, int port, String event) {
        this.ip = ip;
        this.port = port;
        this.event = event;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getEvent() {
        return event;
    }

}
