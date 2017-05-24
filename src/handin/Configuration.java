package handin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public interface Configuration {
    int[] portRange = {10000, 10999};
    int serverPort = 40499;
    // --Commented out by Inspection (21/05/2017 16.44):int connectionAttemptsToNewSequencer = 100;
    // --Commented out by Inspection (21/05/2017 16.44):int waitPerConnectionAttempt = 100;

    /**
     * Gets the IP address of the current machine.
     * The purpose of this method is to avoid getting a loopback address
     *
     * @return The IP address of the current machine. "127.0.0.1" is returned if no IP is found.
     */
    static String getIP() {
        String ip = "127.0.0.1";
        try {
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) en.nextElement();
                Enumeration ee = ni.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress ia = (InetAddress) ee.nextElement();
                    if (!ia.isLoopbackAddress() && !ia.getHostAddress().contains(":")) ip = ia.getHostAddress();
                }
            }
        } catch (SocketException ignored) {
        }
        return ip;
    }
}
