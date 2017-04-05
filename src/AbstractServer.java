import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Made by Rasmus on 05/04/2017.
 */
public class AbstractServer extends AbstractNetworkUnit {


    private ServerSocket serverSocket;

    public AbstractServer(int portNumber) {
        super(portNumber);
    }

    protected void registerOnPort() {
        try {
            serverSocket = new ServerSocket(serverPortNumber);
        } catch (IOException e) {
            serverSocket = null;
            System.err.println("Cannot open server socket on port number" + serverPortNumber);
            System.err.println(e);
            System.exit(-1);
        }
    }

    protected Socket waitForConnectionFromClient() {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }


    protected void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }


}
