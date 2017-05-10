package handin.communication;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Made by Rasmus on 05/04/2017.
 */
public abstract class AbstractNetworkUnit {
    protected final int serverPortNumber;

    protected AbstractNetworkUnit(int serverPortNumber) {
        this.serverPortNumber = serverPortNumber;
    }

    public String getLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }
}
