package handin;

import handin.communication.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TokenThreadHandler implements Runnable {

    private int listenPort = 0;
    private Editor editor;
    private LeaderToken leaderToken;

    public TokenThreadHandler(Editor editor, LeaderToken leaderToken) {
        this.editor = editor;
        this.leaderToken = leaderToken;
    }

    public void resetLeaderToken() {
        this.leaderToken = null;
    }

    @Override
    public void run() {
        int tempListenPort = Configuration.portRange[0];
        //Listen for Token getters.
        Socket tokenSocket;
        Server server = new Server(tempListenPort);
        while (!server.registerOnPort()) {
            tempListenPort++;
            server = new Server(tempListenPort);
        }
        listenPort = tempListenPort;

        editor.DisplayError("Listening on port: " + (tempListenPort));
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
                    System.out.println("Leader Token exchange socket hard closed");
                }
        }
    }

    public LeaderToken getLeaderToken() {
        return leaderToken;
    }

    public int getListenPort() {
        while (listenPort==0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return listenPort;
    }
}
