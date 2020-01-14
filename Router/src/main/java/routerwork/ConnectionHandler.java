package main.java.routerwork;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;

import java.io.*;
import java.nio.charset.*;

public class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
      private static int clientId = 543210;
  @Override
  public void completed(AsynchronousSocketChannel client, Attachment attach) {
    try {
      SocketAddress clientAddr = client.getRemoteAddress();
      System.out.format("Accepted a  connection from  %s%n", clientAddr);
      attach.server.accept(attach, this);
      rwHandler rwHandler = new rwHandler();
      Attachment newAttach = new Attachment();
      newAttach.server = attach.server;
      newAttach.client = client;
      newAttach.clientId = clientId++;
      newAttach.buffer = ByteBuffer.allocate(2048);
      newAttach.isRead = false;
      newAttach.clientAddr = clientAddr;
      Charset cs = Charset.forName("UTF-8");
      byte data [] = Integer.toString(newAttach.clientId).getBytes(cs);
      newAttach.rwHandler = rwHandler;
      newAttach.buffer.put(data);
      newAttach.buffer.flip();
      Router.addClient(newAttach);
      client.write(newAttach.buffer, newAttach, rwHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void failed(Throwable e, Attachment attach) {
    System.out.println("Connection failed.");
    e.printStackTrace();
  }
}
