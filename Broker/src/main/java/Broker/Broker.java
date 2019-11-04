package Broker;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.Future;

public class Broker {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the broker");

        AsynchronousSocketChannel   channel = AsynchronousSocketChannel.open();
        SocketAddress   serverAddr = new InetSocketAddress("localhost", 5000);
        Future<Void>    result = channel.connect(serverAddr);
        result.get();
        System.out.println("Connected");
        Attachment      attach = new Attachment();
        attach.channel = channel;
        attach.buffer = ByteBuffer.allocate(2048);
        attach.isRead = true;
        attach.mainThread = Thread.currentThread();

        ReadWriteHandler    rwHandler = new ReadWriteHandler();
        channel.read(attach.buffer, attach, rwHandler);
        attach.mainThread.join();
    }
}

class Attachment {
    AsynchronousSocketChannel   channel;
    Integer                     ID;
    ByteBuffer                  buffer;
    Thread                      mainThread;
    boolean                     isRead;
}

class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
        if(attach.isRead) {
            attach.buffer.flip();
            byte[]    bytes = new byte[attach.buffer.limit()];
            attach.buffer.get(bytes);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);

            if(message.length() > 0) {
                // get the ID from the charBuffer written to the channel
                if (message.charAt(1) == 'I') {
                    //System.out.println(message);
                    String messageID = message.replaceAll("[^0-9]", "");
                    Integer id = Integer.parseInt(messageID);
                    attach.ID = id;
                } else {
                    String[] messageData = message.split("\\|");
                    if(messageData[2].equals("Executed") || messageData[2].equals("Rejected")) {
                        System.out.format("Market responded with: " + messageData[2] + "\n");
                        System.out.println();
                    } else {
                        System.out.println("Unable to get a response from Market. Try again");
                    }
                }
                message = this.getTextFromUser(attach.ID);

                try {
                    attach.buffer.clear();
                    byte[] data = message.getBytes(cs);
                    attach.buffer.put(data);
                    attach.buffer.flip();
                    attach.isRead = false;
                    attach.channel.write(attach.buffer, attach, this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Connection disconnected");
                System.exit(0);
            }
        } else {
            attach.isRead = true;
            attach.buffer.clear();
            attach.channel.read(attach.buffer, attach, this);
        }
    }

    @Override
    public void failed(Throwable e, Attachment attach) {
        e.printStackTrace();
    }

    private String  getTextFromUser(Integer id) {
        String      message = "";

        // get market ID?
        System.out.println("Please enter a market ID:");
        boolean     validInput = false;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                int         MID = scanner.nextInt();
                if(MID < 543210 || MID == id) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(MID) + "|" + id + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a market ID (ID's start at 543210) ");
            }
        }

        // get BUY / SELL
        System.out.println("Would you like to:\n1. Buy\n2. Sell");
        validInput = false;
        int         option;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                option = scanner.nextInt();
                if(option < 1 || option > 2) {
                    System.out.println("Invalid input");
                } else if(option == 1) {
                    message += "BUY|";
                    validInput = true;
                } else if(option == 2) {
                    message += "SELL|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a number");
            }
        }

        // get Symbol
        System.out.println("Instrument symbol:");
        try {
            Scanner     scanner = new Scanner(System.in);

            message += scanner.nextLine().trim() + "|";
        } catch (InputMismatchException e) {
            System.out.println("Invalid input --> You have not entered a valid instrument");
        }
        // get Price
        System.out.println("Price:");
        validInput = false;
        int         price = 0;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                price = scanner.nextInt();
                if(price < 1) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(price) + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a valid price");
            }
        }

        // get Quantity
        System.out.println("Quantity:");
        validInput = false;
        int         quantity = 0;
        while(!validInput) {
            try {
                Scanner     scanner = new Scanner(System.in);

                quantity = scanner.nextInt();
                if(quantity < 1) {
                    System.out.println("Invalid input");
                } else {
                    message += Integer.toString(quantity) + "|";
                    validInput = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input --> You have not entered a valid quantity");
            }
        }

        // calculate checksum
        int     checksum = 0;
        for(int i = 0; i < message.length(); i++) {
            checksum += message.charAt(i);
        }

        message += Integer.toString(checksum);

        System.out.println(message);

        return message;
    }
}
