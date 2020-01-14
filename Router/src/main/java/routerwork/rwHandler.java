package main.java.routerwork;

import java.io.*;

import java.nio.channels.*;
import java.nio.charset.*;

public class rwHandler implements CompletionHandler<Integer, Attachment> {
    
    private String SOH;
    public rwHandler()
    {
      SOH = "" + (char)1;
    }
    @Override
    public void completed(Integer result, Attachment attach) {
        
      if (result == -1) {
        try {
          attach.client.close();
          Router.removeClient(attach.clientId);
          String port = attach.server.getLocalAddress().toString().split(":")[1];
          System.out.format("[" + getServerName(port) + "] Stopped   listening to the   client %s%n",
              attach.clientAddr);
        } catch (IOException ex) {
          ex.printStackTrace();
        }
        return;
      }
  
      if (attach.isRead) {
        attach.buffer.flip();
        int limits = attach.buffer.limit();
        byte bytes[] = new byte[limits];
        attach.buffer.get(bytes, 0, limits);
        Charset cs = Charset.forName("UTF-8");
        String msg = new String(bytes, cs);
        String datum[] = msg.split(SOH);
        attach.msg = datum;
        try
        {
            String port = attach.server.getLocalAddress().toString().split(":")[1];
            System.out.format("["+ getServerName(port) +"] Client at  %s  says: %s%n", attach.clientAddr,
            msg.replace((char)1, '|'));
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        attach.isRead = false; // It is a write
        attach.buffer.rewind();
        attach.buffer.clear();
        byte[] data = msg.getBytes(cs);
        attach.buffer.put(data);
        attach.buffer.flip();
        if (attach.client.isOpen() && Router.getSize() > 1)
        {
            new CheckSum().performAction(attach, Undependable.CHECKSUM);
        }

  
      } else {
        // Write to the client
        attach.isRead = true;
        attach.buffer.clear();
        attach.client.read(attach.buffer, attach, this);

      }
    }
    @Override
    public void failed(Throwable e, Attachment attach) {
      e.printStackTrace();
    }
    private String getServerName(String port)
    {
        if (port.equals("5000"))
            return "Broker Server";
        else
            return "Market Server";
    }
  }