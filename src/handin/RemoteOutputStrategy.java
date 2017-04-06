package handin;


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class RemoteOutputStrategy implements OutputStrategy {
    private Socket socket;
    private final ObjectOutputStream out;

    public RemoteOutputStrategy(Socket socket) {
        this.socket = socket;
        out = createOutputStream();
    }

    private ObjectOutputStream createOutputStream() {
        try {
            return new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void output(MyTextEvent event) {
        try {
            if (socket.isConnected())
                out.writeObject(event);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
