package exercise3;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class QAServer {
    protected int portNumber = 40499;
    protected ServerSocket serverSocket;
    private boolean full;

    public void run() {
        System.out.println("Hello world!");
        printLocalHostAddress();
        registerOnPort();
        full = false;

        while (true) {

            Socket socket = waitForConnectionFromClient();

            Thread thread = new Thread(() -> {
                if (socket != null) {
                    LinkedBlockingQueue<QA> questions = new LinkedBlockingQueue<>();
                    createQuestionListenerThread(socket, questions).start();

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
                        System.out.println("Socket disconnected");
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
                }
                //TODO fix down here
                deregisterOnPort();
            });
            thread.start();
        }

    }

    public Thread createQuestionListenerThread(Socket socket, LinkedBlockingQueue<QA> questions) {
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

    protected void registerOnPort() {
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            serverSocket = null;
            System.err.println("Cannot open server socket on port number" + portNumber);
            System.err.println(e);
            System.exit(-1);
        }
    }


    protected Socket waitForConnectionFromClient() {
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
                System.err.println(e);
            }
        }
    }

    protected void printLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String localhostAddress = localhost.getHostAddress();
            System.out.println("Contact this server on the IP address " + localhostAddress);
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws IOException {
        QAServer qa = new QAServer();
        qa.run();
    }

}
