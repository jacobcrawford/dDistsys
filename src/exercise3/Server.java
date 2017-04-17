package exercise3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Made by Rasmus on 05/04/2017.
 */
public class Server extends AbstractNetworkUnit {


    private ServerSocket serverSocket;

    public Server(int portNumber) {
        super(portNumber);
    }

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
