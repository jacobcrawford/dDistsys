import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class QAServer {
    protected int portNumber = 40499;
    protected ServerSocket serverSocket;
    protected Thread questionListener;




    public void run() {
        System.out.println("Hello world!");
        printLocalHostAddress();
        registerOnPort();

        while (true) {
            Socket socket = waitForConnectionFromClient();

            if (socket != null) {
                LinkedBlockingQueue questions = new LinkedBlockingQueue();
                if (questionListener == null) {
                    questionListener=createQuestionListenerThread(socket,questions);
                    questionListener.start();
                }

                System.out.println("Connection from " + socket);
                try {

                    ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
                    QA qa;// Read and print what the client is sending
                    InputStreamReader stream = new InputStreamReader(System.in);
                    BufferedReader stdin = new BufferedReader(stream);

                    while (socket.isConnected()) {
                        qa = (QA) questions.take();
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

    public Thread createQuestionListenerThread(Socket socket,LinkedBlockingQueue questions){
        return new Thread(()->{
            try {
                ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                while(socket.isConnected()){
                    questions.add((QA)fromClient.readObject());
                }
                fromClient.close();

            } catch (EOFException e){
                System.out.println("Connection to client was broken");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    protected void registerOnPort(){
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
