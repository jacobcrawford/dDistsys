package exercise3;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Made by Rasmus on 05/04/2017.
 */
public class AbstractNetworkUnit {
    protected int serverPortNumber;

    public AbstractNetworkUnit(int serverPortNumber) {
        this.serverPortNumber = serverPortNumber;
    }

    protected void printLocalHostAddress() {
        String localhostAddress = this.getLocalHostAddress();
        System.out.println("Contact this server on the IP address " + localhostAddress);
    }

    public String getLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
        return null;
    }
}
