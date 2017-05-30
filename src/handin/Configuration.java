package handin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Configuration {
    public static final int connectionTimeout = 10000;
    public static final int waitForNewClientToConnectTimeout = 50;
    static final int[] portRange = {10000, 10999};
    static final int serverPort = 40499;
    static final int connectionAttemptsToNewSequencer = 10;
    static final int waitPerConnectionAttempt = 100;

    private static String ip = null;
    /**
     * Gets the IP address of the current machine.
     * The purpose of this method is to avoid getting a loopback address
     *
     * @return The IP address of the current machine. "127.0.0.1" is returned if no IP is found.
     */
    public static String getIP() {
        if (ip != null) return ip;
        ip = "127.0.0.1";
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

    /**
     * Main method for testing the caching of the getIP() method
     *
     * @param args Ignored
     */
    public static void main(String[] args) {
        long start = System.nanoTime();
        System.out.println("First run:");
        getIP();
        int timeFirstRun = Math.toIntExact(System.nanoTime() - start);
        System.out.println("Time: " + timeFirstRun + "\n");

        System.out.println("Following 10,000 average run:");
        long sum = 0;
        for (int i = 0; i < 10000; i++) {
            long time = System.nanoTime();
            getIP();
            sum += (System.nanoTime() - time);
        }
        int avg = Math.toIntExact(sum / 10000);
        System.out.println("Time: " + avg + "\n");

        System.out.println("Caching improved query time by a factor " + timeFirstRun / avg);
    }
}
