package handin;

import java.io.Serializable;

/**
 * The LeaderToken is a token specifying the sequencer of a network of distributedTextEditors
 */
public class LeaderToken implements Serializable {
    private String ip;
    private int port;

    public LeaderToken(String ip, int port) {
        this.ip = ip;
        this.port = port;
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

    @Override
    public String toString() {
        return "LeaderToken:" + ip + ":" + port;
    }
}
