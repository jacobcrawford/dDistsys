package handin;

import handin.communication.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TokenThreadHandler implements Runnable {

    private int listenPort;
    private Editor editor;
    private LeaderToken leaderToken;
    private boolean broadcastMode;
    private LeaderToken result;

    public TokenThreadHandler(int listenPort, Editor editor, LeaderToken leaderToken) {
        this.listenPort = listenPort;
        this.editor = editor;
        this.leaderToken = leaderToken;
        broadcastMode = true;
    }

    @Override
    public void run() {
        //Listen for Token getters.
        Socket tokenSocket;
        Server server = new Server(listenPort);
        while (!server.registerOnPort()) {
            server = new Server(listenPort);
            listenPort++;
        }

        editor.DisplayError("Listening on port: " + (listenPort));
        while (!Thread.interrupted()) {
            tokenSocket = server.waitForConnectionFromClient();
            if (broadcastMode) {
                try {
                    ObjectOutputStream tokenSender = new ObjectOutputStream(tokenSocket.getOutputStream());
                    tokenSender.writeObject(getLeaderToken());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(tokenSocket.getInputStream());
                    Object input = inputStream.readObject();
                    if (input instanceof LeaderToken) {
                        result = (LeaderToken) input;
                    }
                    inputStream.close();
                    tokenSocket.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private LeaderToken getLeaderToken() {
        return leaderToken;
    }

    public int getListenPort() {
        return listenPort;
    }

    public LeaderToken getNewToken() {
        System.out.println("I'm now ready to receive");
        broadcastMode = false;
        result = null;
        while (result == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        broadcastMode = true;
        return result;
    }
}
