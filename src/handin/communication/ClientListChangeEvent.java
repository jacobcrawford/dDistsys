package handin.communication;

import java.io.Serializable;

public class ClientListChangeEvent implements Event, Serializable {
    private String ip;
    private int port;
    private String event;

    public static String remove = "REMOVE";
    public static String add = "ADD";

    public ClientListChangeEvent(String ip, int port, String event) {
        this.ip = ip;
        this.port = port;
        this.event = event;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}
