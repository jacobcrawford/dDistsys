package handin;

import java.io.Serializable;

/**
 * The LeaderToken is a token specifying the sequencer of a network of distributedTextEditors
 */
public class LeaderToken implements Serializable {
    private final String ip;
    private final int port;

    public LeaderToken(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "LeaderToken:" + ip + ":" + port;
    }
}
