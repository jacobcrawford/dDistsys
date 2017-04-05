package exercise3;

import java.io.IOException;
import java.net.Socket;

/**
 * Made by Rasmus on 05/04/2017.
 */
public class AbstractClient extends AbstractNetworkUnit {

    public AbstractClient(int portNumber) {
        super(portNumber);
    }

    public Socket connectToServer(String serverName) {
        Socket res = null;
        try {
            res = new Socket(serverName, serverPortNumber);
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

}
