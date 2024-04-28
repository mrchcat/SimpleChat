package utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection implements AutoCloseable {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void close()  {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println("Ошибка закрытия ресурсов");
        }
    }

    public void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    public Message receive() throws IOException, ClassNotFoundException {
        if (in.readObject() instanceof Message message){
            return message;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
