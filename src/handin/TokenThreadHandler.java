package handin;

import handin.communication.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class TokenThreadHandler implements Runnable {

    private final Editor editor;
    private int listenPort = 0;
    private LeaderToken leaderToken;
    private Semaphore semaphore;

    public TokenThreadHandler(Editor editor, LeaderToken leaderToken, Semaphore semaphore) {
        this.editor = editor;
        this.leaderToken = leaderToken;
        this.semaphore = semaphore;
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
        semaphore.release();
        System.out.println("Released");
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
        return listenPort;
    }
}
