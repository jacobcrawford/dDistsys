package exercise3;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Made by Rasmus on 05/04/2017.
 */
public class AbstractNetworkUnit {
    protected int serverPortNumber;

    public AbstractNetworkUnit(int serverPortNumber){
        this.serverPortNumber = serverPortNumber;
    }

    protected void printLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String localhostAddress = localhost.getHostAddress();
            System.out.println("Contact this server on the IP address " + localhostAddress);
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
    }

}
