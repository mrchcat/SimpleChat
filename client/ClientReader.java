package client;

import utils.Connection;
import utils.Message;

import java.io.IOException;

public class ClientReader extends Thread {
    private final Connection connection;
    private volatile boolean stop = false;

    public ClientReader(Connection connection) {
        this.connection = connection;
    }

    public boolean isStop() {
        return stop;
    }

    static ClientReader getAndStart(Connection connection) {
        ClientReader clientReader = new ClientReader(connection);
        clientReader.start();
        return clientReader;
    }


    public void stopReader() {
        this.stop = true;
    }

    @Override
    public void run() {
        Message answer;
        try {
            while (!stop) {
                answer = connection.receive();
                switch (answer.getType()) {
                    case POST, POST_DENIED -> System.out.println(answer.getAuthor() + ": " + answer.getBody());
                    case USER_ADDED -> System.out.println(answer.getBody() + " вошел в чат");
                    case USER_REMOVED -> System.out.println(answer.getBody() + " вышел из чата");
                    case CONNECTION_CLOSED -> stopReader();
                    case USERS -> {
                        int n = answer.getBody().split(";").length;
                        System.out.println("Сейчас в чате " + n + " участник(-ов): " + answer.getBody());
                    }
                    case HISTORY -> {
                        System.out.println("История сообщений:");
                        System.out.println(answer.getBody());
                    }
                    default -> System.out.println("Неизвестная команда сервера");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка соединения");
        }
    }
}
