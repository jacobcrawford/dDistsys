import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class QAClient {

    protected int portNumber = 40499;
    protected Thread answerListener;

    public void run(String serverName) {

        System.out.println("Hello world!");

        printLocalHostAddress();

        Socket socket = connectToServer(serverName);

        if (socket != null) {
            System.out.println("Connected to " + socket);
            if (answerListener ==null){
                answerListener = getAnswerListener(socket);
                answerListener.start();
            }

            try {
                final LinkedBlockingQueue<QA> qaQueue = new LinkedBlockingQueue<>();

                final ObjectOutputStream questionStream = new ObjectOutputStream(socket.getOutputStream());
                // For reading from standard input
                Thread questionSender = new Thread(()->{
                   while (socket.isConnected()){
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
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(),true);
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

    public Thread getAnswerListener(Socket socket){
        return new Thread(()->{
            try {
                ObjectInputStream answerStream = new ObjectInputStream(socket.getInputStream());
                while (socket.isConnected()){
                    QA answer = (QA)answerStream.readObject();
                    System.out.println("The answer to question \""
                            +answer.getQuestion()+"\""+" is "+"\""
                            +answer.getAnswer()+"\"");

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    protected void printLocalHostAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String localhostAddress = localhost.getHostAddress();
            System.out.println("I'm a client running with IP address " + localhostAddress);
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve the Internet address of the local host.");
            System.err.println(e);
            System.exit(-1);
        }
    }

    protected Socket connectToServer(String serverName) {
        Socket res = null;
        try {
            res = new Socket(serverName,portNumber);
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    public static void main(String[] args) {
        QAClient qa = new QAClient();
        qa.run(args[0]);
    }

}
