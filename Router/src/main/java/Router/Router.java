package Router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Router {

    private static Integer  IDcurr;
    private static HashMap<Integer, Attachment>  routingTable = new HashMap<Integer, Attachment>();
    private static String   host = "localhost";
    private static int      brokerPort = 5000;
    private static int      marketPort = 5001;
    // store sockets in some sort of list of ConnectionAttachments

    // main function
    public static void main(String[] args) throws Exception {
        System.out.println("Server has been started");
        //This is a Starting unique ID
        IDcurr = 543210;

        //create a new socket for each
        AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open();
        InetSocketAddress brokerHost = new InetSocketAddress(host, brokerPort);
        brokerChannel.bind(brokerHost);

        AsynchronousServerSocketChannel marketChannel;
        InetSocketAddress marketHost = new InetSocketAddress(host, marketPort);
        marketChannel = AsynchronousServerSocketChannel.open().bind(marketHost);

        //sockets
        System.out.println("Server is listening to port " + brokerHost.getPort());
        System.out.println("Server is listening to port " + marketHost.getPort());

        //attaching multiple channels...
        Attachment attachMarket = new Attachment();
        attachMarket.serverChannel = marketChannel;
        marketChannel.accept(attachMarket, new MarketConnectionHandler());

        Attachment attachBroker = new Attachment();
        attachBroker.serverChannel = brokerChannel;
        brokerChannel.accept(attachBroker, new BrokerConnectionHandler());
        Thread.currentThread().join();

        // is thread interrupted, then exit
        if(Thread.currentThread().isInterrupted()) {
            //Should close
            return ;
        }
    }

    // attachment holds all the attachment properties
    private static class Attachment {
        AsynchronousServerSocketChannel     serverChannel;
        AsynchronousSocketChannel           clientChannel;
        ByteBuffer                          buffer;
        SocketAddress                       clientAddress;
        Boolean                             isRead;
        Integer                             ID = null;
        String                              connectionType;
    }

    private static class BrokerConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
        @Override
        public void completed(AsynchronousSocketChannel client, Attachment attach) {
            try {
                SocketAddress       clientAddr = client.getRemoteAddress();
                attach.serverChannel.accept(attach, this);
                ReadWriteHandler    rwHandler = new ReadWriteHandler();
                Attachment          newAttach = new Attachment();

                newAttach.serverChannel = attach.serverChannel;
                newAttach.clientChannel = client;
                newAttach.buffer = ByteBuffer.allocate(2048);
                newAttach.isRead = false;
                newAttach.ID = IDcurr;
                newAttach.clientAddress = clientAddr;
                newAttach.connectionType = "Broker";

                routingTable.put(newAttach.ID, newAttach);
                System.out.println(newAttach.ID + " connected");

                // write ID to buffer to read ID in instance
                CharBuffer charBuffer = newAttach.buffer.asCharBuffer();
                charBuffer.put("ID:"+ newAttach.ID);
                charBuffer.flip();

                System.out.format("Accepted a Broker connection from %s%n", clientAddr);
                System.out.println("Attachment created: " + IDcurr + "\n");

                newAttach.clientChannel.write(newAttach.buffer);
                newAttach.clientChannel.read(newAttach.buffer, newAttach, rwHandler);
                IDcurr++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable e, Attachment attach) {
            System.out.println("Failed to accept a connection.");
            e.printStackTrace();
        }
    }

    private static class MarketConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Attachment> {
        @Override
        public void completed(AsynchronousSocketChannel client, Attachment attach) {
            try {
                SocketAddress   clientAddr = client.getRemoteAddress();
                attach.serverChannel.accept(attach, this);
                Attachment          newAttach = new Attachment();

                newAttach.serverChannel = attach.serverChannel;
                newAttach.clientChannel = client;
                newAttach.buffer = ByteBuffer.allocate(2048);
                newAttach.isRead = false;
                newAttach.ID = IDcurr;
                newAttach.clientAddress = clientAddr;
                newAttach.connectionType = "Market";

                routingTable.put(newAttach.ID, newAttach);
                System.out.println(newAttach.ID + " connected");

                // write ID to buffer to read ID in instance
                CharBuffer charBuffer = newAttach.buffer.asCharBuffer();
                charBuffer.put("ID:"+ newAttach.ID);
                charBuffer.flip();

                System.out.format("Accepted a Market connection from %s%n", clientAddr);
                System.out.println("Attachment created: " + IDcurr + "\n");
                newAttach.clientChannel.write(newAttach.buffer);
                IDcurr++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable e, Attachment attach) {
            System.out.println("Failed to accept a connection.");
            e.printStackTrace();
        }
    }

    private static Attachment   getAttachment(Integer key) {
        for(Map.Entry<Integer, Attachment> entry : routingTable.entrySet()) {
            if(entry.getValue().ID.equals(key)) {
                return entry.getValue();
            }
        }
        return new Attachment();
    }

    private static Integer      checkMessage(String message) {
        System.out.println("init message: " + message);
        String[]    messageData = message.split("\\|");
        int         checksumTotal = Integer.parseInt(messageData[messageData.length - 1]);

        int     check = 0;
        for(int a = 0; a < messageData.length - 1; a++) {
            for (int b = 0; b < messageData[a].length(); b++) {
                check += messageData[a].charAt(b);
            }
            check += '|';
        }

        if(checksumTotal == check) {
            return Integer.parseInt(messageData[0]);
        }
        return -1;
    }

    private static class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
        @Override
        public void completed(Integer result, Attachment attach) {
            if(result == -1) {
                try {
                    // remove connection from routing table
                    routingTable.remove(attach.ID);
                    attach.clientChannel.close();
                    System.out.format("Stopped listening to the client %s%n", attach.clientAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if(attach.isRead) {
                attach.buffer.flip();
                byte    bytes[] = new byte[attach.buffer.limit()];
                attach.buffer.get(bytes);
                Charset cs = Charset.forName("UTF-8");
                String  message = new String(bytes, cs);

                // check message for invalid input
                Integer     check = checkMessage(message);
                if(check == -1) {
                    // invalid message
                    attach.isRead = false;
                } else {
                    // get attachment id that needs to receive a message
                    // find id in message
                    // sendToID|fromID|BUY/SELL|SYMBOL|PRICE|QUANTITY|CHECKSUM
                    // message example = 1008|1200|sell|akm|12|12|1955
                    // create a new attachment from attachment found
                    Attachment  send = getAttachment(check);
                    if(send.ID == null || (send.connectionType.equals("Broker") && attach.connectionType.equals("Broker")) || (send.connectionType.equals("Market") && attach.connectionType.equals("Market"))) {
                        send = attach;
                        System.out.println("SENDING ID: " + send.ID);
                    }
                    System.out.format("Client at %s says: %s%n", attach.clientAddress, message);
                    send.buffer.clear();
                    byte[]  data = message.getBytes(cs);
                    send.buffer.put(data);
                    send.buffer.flip();
                    attach.isRead = false;
                    attach.buffer.rewind();
                    // send message to broker... using broker ID... from router table
                    System.out.println("Sending to ID: " + send.ID + " with message " + message);
                    send.clientChannel.write(send.buffer, send, this);
                }
            } else {
                // write to the client
                attach.isRead = true;
                attach.buffer.clear();
                attach.clientChannel.read(attach.buffer, attach, this);
            }
        }

        @Override
        public void failed(Throwable e, Attachment attach) {
            e.printStackTrace();
        }
    }
}
// router opens connections for Brokers and Markets on specific Ports 5000 and 5001
// it then relays messages between the two sockets
