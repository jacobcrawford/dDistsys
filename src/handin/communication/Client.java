package handin.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static handin.Configuration.connectionTimeout;


public class Client extends AbstractNetworkUnit {

    public Client(int portNumber) {
        super(portNumber);
    }

    public Socket connectToServer(String serverName) {
        Socket res = null;
        try {
//            res = new Socket(serverName, serverPortNumber);
            res = new Socket();
            res.connect(new InetSocketAddress(serverName, serverPortNumber), connectionTimeout);
        } catch (IOException e) {
            System.out.println("Failed to connect a socket in Client");
            // We return null on IOExceptions
        }
        return res;
    }

}
