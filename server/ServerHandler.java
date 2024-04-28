package server;

import utils.Connection;
import utils.Message;

import java.io.IOException;
import java.net.Socket;

import static java.util.Objects.nonNull;
import static utils.MessageType.*;

public class ServerHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private static int ATTEMPTS ;
    private static String NAME_REGEX;
    private String name = "";

    public ServerHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public static void setATTEMPTS(int ATTEMPTS) {
        ServerHandler.ATTEMPTS = ATTEMPTS;
    }

    public static void setNameRegex(String nameRegex) {
        NAME_REGEX = nameRegex;
    }

    @Override
    public void run() {
        try (Connection connection = new Connection(socket)) {
            if (handshake(connection)) {
                System.out.println("handshake завершен успешно");
                conversation(connection);
                connection.send(new Message(CONNECTION_CLOSED));
                server.removeUserAndNotifyAll(name);
            }
        } catch (IOException | ClassNotFoundException e) {
            server.removeUserAndNotifyAll(name);
        }
    }

    private boolean handshake(Connection connection) throws IOException, ClassNotFoundException {
        System.out.println("Запущен handshake c "+socket.getInetAddress().getHostAddress());
        Message answer;
        boolean connect = true;
        int effort = ATTEMPTS;
        while (connect & (effort > 0)) {
            effort--;
            connection.send(new Message(NAME_REQUEST));
            answer = connection.receive();
            switch (answer.getType()) {
                case USER_NAME -> {
                    name = answer.getBody();
                    if (nonNull(name) && name.matches(NAME_REGEX) && server.isUserUnique(name)) {
                        connection.send(new Message(NAME_ACCEPTED, "Server", name));
                        server.addUserAndNotifyAll(name, connection);
                        return true;
                    }
                }
                case CLOSE_CONNECTION -> connect = false;
            }
            connection.send(new Message(NAME_DENIED,
                    "Server",
                    "Имя " + name + " отклонено. Имя должно соответствовать шаблону:" +
                            " \"" + NAME_REGEX + "\". Осталось " + effort + " попыток."));
        }
        return false;
    }

    private void conversation(Connection connection) throws IOException, ClassNotFoundException {
        System.out.println("Запущен разговор c пользователем " + name);
        connection.send(new Message(USERS, "Server", server.getUsers()));
        String messages = server.getMessages();
        if (!messages.isEmpty()) {
            connection.send(new Message(HISTORY, "Server", messages));
        }
        String body;
        Message text;
        boolean connect = true;
        while (connect) {
            text = connection.receive();
            System.out.println(text);
            switch (text.getType()) {
                case POST -> {
                    body = text.getBody();
                    if (nonNull(body) && !body.isEmpty()) {
                        server.publicPost(new Message(POST, name, body));
                    } else {
                        connection.send(new Message(POST_DENIED, "Server", "Пустое сообщение не допустимо"));
                    }
                }
                case GET_USERS -> connection.send(new Message(USERS, "Server", server.getUsers()));
                case CLOSE_CONNECTION -> connect = false;
                default -> connection.send(new Message(POST_DENIED, "Server", "Некорректный тип сообщения"));
            }
        }
    }
}


