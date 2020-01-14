package main.java.market;

import java.nio.charset.*;
import java.nio.channels.*;
import java.io.*;

public class rwHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
      if (result == -1) {

        attach.mainThread.interrupt();
        System.out.println("Server shutdown unexpectedly, Market going offline...");
        return ;
      }

      if (attach.isRead) {
        attach.buffer.flip();
        Charset cs = Charset.forName("UTF-8");
        int limits = attach.buffer.limit();
        byte bytes[] = new byte[limits];
        attach.buffer.get(bytes, 0, limits);
        String msg = new String(bytes, cs);
        if (attach.clientId == 0) {
          attach.clientId = Integer.parseInt(msg);
          System.out.println("Server Responded with Id: " + attach.clientId);
          attach.isRead = false;
          attach.client.read(attach.buffer, attach, this);
          return ;
        } else
            System.out.println("Server Responded: "+ msg.replace((char)1, '|'));

        attach.buffer.clear();
        msg = Market.processRequest(msg);

        if (msg.contains("bye")) {
            attach.mainThread.interrupt();
            return;
        }

        try {
          System.out.println("\nMarket Response: "+ msg.replace((char)1, '|'));
        } catch (Exception e) {
         
        }

        byte[] data = msg.getBytes(cs);
        attach.buffer.put(data);
        attach.buffer.flip();
        attach.isRead = false; // It is a write
        attach.client.write(attach.buffer, attach, this);
      } else {
        attach.isRead = true;
        attach.buffer.clear();
        attach.client.read(attach.buffer, attach, this);
      }
    }

    @Override
    public void failed(Throwable e, Attachment attach) {
      e.printStackTrace();
    }

    private String getTextFromUser() throws Exception {
      System.out.print("Please enter a  message  (Bye  to quit):");
      BufferedReader consoleReader = new BufferedReader(
          new InputStreamReader(System.in));
      String msg = consoleReader.readLine();
      return msg;
    }

    private String testMe() {
      String soh = "" + (char)1;
      String msg = "id="+543210+soh+"56="+100001+soh+"msg=from market"+soh;
      return msg + getCheckSum(msg);
    }

    private String getCheckSum(String msg) {

        int j = 0;
        char t[];

        String soh = "" + (char)1;
        String datum[] = msg.split(soh);
        for(int k = 0; k < datum.length; k++) {
          t = datum[k].toCharArray();
          for(int i = 0; i < t.length; i++) {
            j += (int)t[i];
          }
          j += 1;
        }
        return ("10="+ (j % 256) + soh);
    }
    private static int i = 0;
  }