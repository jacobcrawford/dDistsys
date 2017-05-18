package handin;

import handin.communication.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TokenThreadHandler implements Runnable {

    private int listenPort;
    private Editor editor;

    public void setLeaderToken(LeaderToken leaderToken) {
        this.leaderToken = leaderToken;
    }

    private LeaderToken leaderToken;

    public TokenThreadHandler(int listenPort, Editor editor, LeaderToken leaderToken) {
        this.listenPort = listenPort;
        this.editor = editor;
        this.leaderToken = leaderToken;
    }

    @Override
    public void run() {
        //Listen for Token getters.
        Socket tokenSocket;
        Server server = new Server(listenPort);
        while (!server.registerOnPort()) {
            listenPort++;
            server = new Server(listenPort);
        }

        editor.DisplayError("Listening on port: " + (listenPort));
        while (!Thread.interrupted()) {
            tokenSocket = server.waitForConnectionFromClient();
                try {
                    ObjectOutputStream tokenSender = new ObjectOutputStream(tokenSocket.getOutputStream());
                    tokenSender.writeObject(getLeaderToken());
                    ObjectInputStream inputStream = new ObjectInputStream(tokenSocket.getInputStream());
                    Object input = inputStream.readObject();
                    System.out.println("got something!");
                    if (input instanceof LeaderToken)
                    {
                        leaderToken = (LeaderToken) input;
                        System.out.println("GOT A LEADER TOKEN");
                    }
                    tokenSocket.close();
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("TokenThread " +Thread.currentThread().getName() + "Unexpected closure");
                }
        }
    }

    public LeaderToken getLeaderToken() {
        return leaderToken;
    }

    public int getListenPort() {
        return listenPort;
    }
}
