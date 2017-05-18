package handin;

import handin.communication.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TokenThreadHandler implements Runnable {

    private int listenPort;
    private Editor editor;
    private Object leaderToken;
    private boolean listening;
    private Server server;

    public TokenThreadHandler(int listenPort, Editor editor, Socket socket, Object leaderToken) {
        this.listenPort = listenPort;
        this.editor = editor;
        this.leaderToken = leaderToken;
    }

    @Override
    public void run() {
        //Listen for Token getters.
        Socket tokenSocket;
        server = new Server(listenPort);
        while (!server.registerOnPort()) {
            server = new Server(listenPort);
            listenPort++;
        }

        editor.DisplayError("Listening on port: " + (listenPort));
        while (!Thread.interrupted()) {
            if(listening) {
                tokenSocket = server.waitForConnectionFromClient();
                try {
                    ObjectOutputStream tokenSender = new ObjectOutputStream(tokenSocket.getOutputStream());
                    tokenSender.writeObject(getLeaderToken());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object getLeaderToken() {
        return leaderToken;
    }

    public int getListenPort() {
        return listenPort;
    }

    public LeaderToken getNewToken() {
        listening = false;
        LeaderToken result = null;
        while(result==null) {
            Socket tokenSocket = server.waitForConnectionFromClient();
            try {
                ObjectInputStream inputStream = new ObjectInputStream(tokenSocket.getInputStream());
                Object input = inputStream.readObject();
                if (input instanceof LeaderToken)
                {
                    result = (LeaderToken) input;
                }
                inputStream.close();
                tokenSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        listening = true;
        return result;
    }
}
