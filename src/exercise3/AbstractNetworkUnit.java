package exercise3;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Made by Rasmus on 05/04/2017.
 */
public abstract class AbstractNetworkUnit {
    protected int serverPortNumber;

    public AbstractNetworkUnit(int serverPortNumber) {
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
