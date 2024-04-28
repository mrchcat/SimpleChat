package client;

import utils.Connection;
import utils.Message;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Scanner;

import static utils.MessageType.*;

public class Client {

    static {
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(Path.of("src", "utils", "config.properties")));
            port = Integer.parseInt(props.getProperty("port"));
            host = props.getProperty("host");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String host;
    private static final int port;
    private final Scanner scanner;
    private static final String STOP_WORD ="/exit";
    private static final String GET_USERS_WORD ="/users";
    private static final String HELP_WORD ="/help";


    public Client() {
        this.scanner = new Scanner(System.in);
    }

    public static void getAndStart() {
        Client client = new Client();
        System.out.println("Клиент запущен");
        client.start();
    }

    private void printHelp(){
        System.out.println("-".repeat(20));
        System.out.println("Справка");
        System.out.println("Для запроса активных пользователей чата введите " + GET_USERS_WORD);
        System.out.println("Для прекращения работы программы введите " + STOP_WORD);
        System.out.println("Для получения справки введите" + HELP_WORD);
        System.out.println("-".repeat(20));
    }

    public void start() {
        try (Socket socket = new Socket(host, port); Connection connection = new Connection(socket)) {
            System.out.println("Установлено соединение с сервером");
            printHelp();
            if (handshake(connection)) {
                conversation(connection);
            } else {
                System.out.println("Соединение с сервером не установлено");
            }
        } catch (UnknownHostException e) {
            System.out.println("Неизвестный хост: " + host);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка соединения");
        } finally {
            System.out.println("Клиент остановлен");
        }
    }

    private boolean handshake(Connection connection) throws IOException, ClassNotFoundException {
        Message answer;
        boolean connect = true;
        while (connect) {
            answer = connection.receive();
            switch (answer.getType()) {
                case NAME_REQUEST -> {
                    System.out.println("Введите свое имя:");
                    String name = scanner.next().trim();
                    if (name.equals(STOP_WORD)) {
                        connection.send(new Message(CLOSE_CONNECTION));
                        connect = false;
                    } else {
                        connection.send(new Message(USER_NAME, "Server", name));
                    }
                }
                case NAME_ACCEPTED -> {
                    System.out.println("Имя " + answer.getBody() + " подтверждено сервером");
                    return true;
                }
                case NAME_DENIED -> System.out.println(answer.getBody());
                case CLOSE_CONNECTION -> connect = false;
            }
        }
        return false;
    }

    public void conversation(Connection connection) {
        try {
            ClientReader reader = ClientReader.getAndStart(connection);
            while (!reader.isStop()) {
                String text = scanner.nextLine().trim();
                if (text.isEmpty()) continue;
                switch (text) {
                    case STOP_WORD -> {
                        connection.send(new Message(CLOSE_CONNECTION));
                        reader.stopReader();
                        reader.join();
                        return;
                    }
                    case GET_USERS_WORD -> connection.send(new Message(GET_USERS));
                    case HELP_WORD->printHelp();
                    default -> connection.send(new Message(POST, "", text));
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка соединения");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
