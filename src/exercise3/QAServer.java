package exercise3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class QAServer extends AbstractServer {
    private int portNumber;
    private ServerSocket serverSocket;
    private Thread questionListener;

    public QAServer(int serverPortNumber) {
        super(serverPortNumber);
    }

    public static void main(String[] args) throws IOException {
        QAServer qa = new QAServer(40499);
        qa.run();
    }

    private void run() {
        System.out.println("Hello world!");
        printLocalHostAddress();
        registerOnPort();

        while (true) {
            Socket socket = waitForConnectionFromClient();

            if (socket != null) {
                LinkedBlockingQueue<QA> questions = new LinkedBlockingQueue<>();
                if (questionListener == null) {
                    questionListener = createQuestionListenerThread(socket, questions);
                    questionListener.start();
                }

                System.out.println("Connection from " + socket);
                try {

                    ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
                    QA qa;// Read and print what the client is sending
                    InputStreamReader stream = new InputStreamReader(System.in);
                    BufferedReader stdin = new BufferedReader(stream);

                    while (socket.isConnected()) {
                        qa = questions.take();
                        //Empty the system.in stream so we only get the string after the question is asked.
                        while (stream.ready()) {
                            String ans = stdin.readLine();
                        }
                        System.out.println("Question: " + qa.getQuestion());
                        qa.setAnswer(stdin.readLine());
                        toClient.writeObject(qa);
                    }
                    //TODO close socket
                } catch (IOException e) {
                    // We report but otherwise ignore IOExceptions
                    System.err.println(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Connection closed by client.");
            } else {
                System.out.println("DISCONNECTED");
                // We rather agressively terminate the server on the first connection exception
                break;
            }
            //TODO fix down here
            deregisterOnPort();
        }
        System.out.println("Goodbuy world!");
    }

    private Thread createQuestionListenerThread(Socket socket, LinkedBlockingQueue<QA> questions) {
        return new Thread(() -> {
            try {
                ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                while (socket.isConnected()) {
                    Object o = fromClient.readObject();
                    if (o instanceof QA) {
                        QA qa = (QA) o;
                        questions.add(qa);
                    } else {
                        System.out.println("Object from client was not exercise3.QA");
                    }
                }
                fromClient.close();

            } catch (EOFException e) {
                System.out.println("Connection to client was broken");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }




}
