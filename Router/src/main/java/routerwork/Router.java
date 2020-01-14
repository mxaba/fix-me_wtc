package main.java.routerwork;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.*;
public class Router {
    private String _host;
    private int _port;
    private static List<Attachment> clients = new ArrayList<Attachment>();

    public Router(String host, int port) {
        _host = host;
        _port = port;
    }

    public static void addClient(Attachment client) {
        clients.add(client);
    }

    public static Attachment getClient(int id) {
        for(Attachment client : clients)
            {
                if (client.clientId == id)
                    return client;
            }
            return null;
    }

    public static int getSize() {
        return clients.size();
    }

    public void startServers() throws Exception {
        ExecutorService threads = Executors.newCachedThreadPool();
        threads.submit(new StartServer(_host, _port));
        threads.submit(new StartServer(_host, _port + 1));
        threads.shutdown();
    }

    public static void removeClient(int id) {

        try {
            clients.remove(getClient(id));
        } catch(Exception e) {
            
        }
    }
}