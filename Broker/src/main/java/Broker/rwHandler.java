package main.java.broker;

import java.nio.charset.*;
import java.nio.channels.*;

public class rwHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
      if (result == -1)
        {
          attach.mainThread.interrupt();
          System.out.println("Server shutdown unexpectedly, Broker shutting down...");
          return ;
        }
      if (attach.isRead) {
        attach.buffer.flip();
        Charset cs = Charset.forName("UTF-8");
        int limits = attach.buffer.limit();
        byte bytes[] = new byte[limits];
        attach.buffer.get(bytes, 0, limits);
        String msg = new String(bytes, cs);
        if (attach.clientId == 0)
        {
          attach.clientId = Integer.parseInt(msg);
          System.out.println("Server responded with Id: " + attach.clientId);
        }
        else
          System.out.println("Server Responded: "+ msg.replace((char)1, '|'));
        try {
          boolean s = Broker.proccessReply(msg);
          if (s == true && Broker.bs == 1)
            Broker.updateData(true);
          if (s == true && Broker.bs == 0)
            Broker.updateData(false);
        } catch (Exception e) {
          e.printStackTrace();
        }
        attach.buffer.clear();
        msg = testMe(attach);
        if (msg.contains("bye") || i > 3) {
          attach.mainThread.interrupt();
          return;
        }
        i++;
        System.out.println("\nBroker response:" + msg.replace((char)1, '|'));
        byte[] data = msg.getBytes(cs);
        attach.buffer.put(data);
        attach.buffer.flip();
        attach.isRead = false; // It is a write
        attach.client.write(attach.buffer, attach, this);
      }else {
        attach.isRead = true;
        attach.buffer.clear();
        attach.client.read(attach.buffer, attach, this);
      }
    }
    @Override
    public void failed(Throwable e, Attachment attach) {
      e.printStackTrace();
    }
    private String testMe(Attachment attach)
    {
      String msg;
      
      if (Broker.bs == 1)
        msg = Broker.buyProduct(Broker.dstId);
      else
        msg = Broker.sellProduct(Broker.dstId);
      return msg + getCheckSum(msg);
    }
    private String getCheckSum(String msg)
    {
        int j = 0;
        char t[];
        String soh = "" + (char)1;
        String datum[] = msg.split(soh);
        for(int k = 0; k < datum.length; k++)
        {
          t = datum[k].toCharArray();
          for(int i = 0; i < t.length; i++)
          {
            j += (int)t[i];
          }
          j += 1;
        }
        return ("10="+ (j % 256) + soh);
    }
    private static int i = 0;
  }