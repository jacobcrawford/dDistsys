import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class QAClient extends AbstractClient {

    private Thread answerListener;

    public QAClient(int portNumber) {
        super(portNumber);
    }

    public static void main(String[] args) {
        QAClient qa = new QAClient(40499);
        qa.run(args[0]);
    }

    private void run(String serverName) {

        System.out.println("Hello world!");

        printLocalHostAddress();

        Socket socket = connectToServer(serverName);

        if (socket != null) {
            System.out.println("Connected to " + socket);
            if (answerListener == null) {
                answerListener = getAnswerListener(socket);
                answerListener.start();
            }

            try {
                final LinkedBlockingQueue<QA> qaQueue = new LinkedBlockingQueue<>();

                final ObjectOutputStream questionStream = new ObjectOutputStream(socket.getOutputStream());
                // For reading from standard input
                Thread questionSender = new Thread(() -> {
                    while (socket.isConnected()) {
                        try {
                            questionStream.writeObject(qaQueue.take());
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                questionSender.start();
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                // For sending text to the server
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                String s;

                // Read from standard input and send to server
                while ((s = stdin.readLine()) != null && !toServer.checkError()) {
                    QA current = new QA();
                    current.setQuestion(s);
                    qaQueue.add(current);
                }

                socket.close();
            } catch (IOException e) {
                // We ignore IOExceptions
            }
        }

        System.out.println("Goodbuy world!");
    }

    private Thread getAnswerListener(Socket socket) {
        return new Thread(() -> {
            try {
                ObjectInputStream answerStream = new ObjectInputStream(socket.getInputStream());
                while (socket.isConnected()) {
                    QA answer = (QA) answerStream.readObject();
                    System.out.println("The answer to question \""
                            + answer.getQuestion() + "\"" + " is " + "\""
                            + answer.getAnswer() + "\"");

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }


}
