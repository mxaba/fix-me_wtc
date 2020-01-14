package main.java.routerwork;

import java.nio.channels.*;
import java.net.*;

public class StartServer implements Runnable
{
    private String _host;
	private int _port;
	public StartServer(String host, int port) {
        _host = host;
        _port = port;
    }

    @Override
    public void run() {
        try {
            AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open();//w w w  .  j  a  v  a2s .com
            InetSocketAddress sAddr = new InetSocketAddress(_host, _port);
            server.bind(sAddr);
            if (_port % 2 == 0)
                System.out.format("Broker Server is listening at %s%n", sAddr);
            else
                System.out.format("Market Server is listening at %s%n", sAddr);
            Attachment attach = new Attachment();
            attach.server = server;
            server.accept(attach, new ConnectionHandler());
            Thread.currentThread().join();
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}