package server;

import utils.Connection;
import utils.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static utils.MessageType.USER_ADDED;
import static utils.MessageType.USER_REMOVED;

public class Server extends Thread {

    static {
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(Path.of("src", "utils", "config.properties")));
            PORT = Integer.parseInt(props.getProperty("port"));
            MAX_SOCKETS = Integer.parseInt(props.getProperty("max_sockets"));
            dequeLimit = Integer.parseInt(props.getProperty("dequeLimit"));
            ServerHandler.setATTEMPTS(Integer.parseInt(props.getProperty("nameEnterAttempts")));
            ServerHandler.setNameRegex(props.getProperty("nameTemplate.regexp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int PORT;
    private static final int MAX_SOCKETS;
    private final Map<String, Connection> users;
    private final Deque<Message> messages;
    private final static int dequeLimit;
    private final AtomicInteger dequeSize;

    private final Semaphore semaphore;
    private final ExecutorService executor;
    private volatile boolean isStopped = false;


    private Server() {
        this.users = new ConcurrentHashMap<>();
        this.messages = new ConcurrentLinkedDeque<>();
        this.semaphore = new Semaphore(MAX_SOCKETS);
        this.executor = Executors.newFixedThreadPool(MAX_SOCKETS);
        this.dequeSize=new AtomicInteger(0);
    }

    public static Server getAndStart() {
        Server server = new Server();
        server.start();
        return server;
    }

    public void stopServer() {
        isStopped = true;
    }

    @Override
    public void run() {
        System.out.println("Сервер запущен на порту " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!isStopped) {
                semaphore.acquire();
                Socket socket = serverSocket.accept();
                System.out.println("Установлено TCP соединение с сокетом "
                        + socket.getInetAddress().getHostAddress()
                        + ": "
                        + socket.getPort());
                executor.submit(new ServerHandler(this, socket));
            }
            System.out.println("Сервер завершил работу");
        } catch (IOException|InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    public boolean isUserUnique(String name) {
        return !users.containsKey(name);
    }

    public String getUsers() {
        return String.join(";", users.keySet());
    }

    public void sendBroadcastMessage(Message message) {
        for (Connection connection : users.values()) {
            try {
                connection.send(message);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void addUserAndNotifyAll(String name, Connection connection) {
        users.put(name, connection);
        sendBroadcastMessage(new Message(USER_ADDED, "Server", name));
    }

    public void removeUserAndNotifyAll(String name) {
        if (users.containsKey(name)) {
            users.remove(name);
            sendBroadcastMessage(new Message(USER_REMOVED, "Server", name));
        }
    }

    public void publicPost(Message message){
        sendBroadcastMessage(message);
        addToMessages(message);
    }

    private void addToMessages(Message message) {
        System.out.println("добавляем в очередь " + message);
        if (dequeSize.incrementAndGet() > dequeLimit) {
            messages.removeFirst();
        }
        messages.addLast(message);
    }

    public String getMessages() {
        if (dequeSize.get() == 0) return "";
        else return messages.stream().map(u -> u.getAuthor() + ": " + u.getBody()).collect(Collectors.joining("\n"));
    }
}
