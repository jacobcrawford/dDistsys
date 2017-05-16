package handin.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends AbstractNetworkUnit {


    private ServerSocket serverSocket;


    public Server(int portNumber) {
        super(portNumber);
    }

    /**
     * Register on the given port.
     *
     * @return If the port was free to register on.
     */
    public boolean registerOnPort() {
        try {
            serverSocket = new ServerSocket(serverPortNumber);
            return true;
        } catch (IOException e) {
            serverSocket = null;
            return false;
        }
    }

    public Socket waitForConnectionFromClient() {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }


    public void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
