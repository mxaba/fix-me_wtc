package Market;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

import org.json.JSONObject;

public class Market {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting the market");

        AsynchronousSocketChannel   channel = AsynchronousSocketChannel.open();
        SocketAddress   serverAddr = new InetSocketAddress("localhost", 8181);
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
    Integer                     ID;
    AsynchronousSocketChannel   channel;
    ByteBuffer                  buffer;
    Thread                      mainThread;
    boolean                     isRead;
}

class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
    @Override
    public void completed(Integer result, Attachment attach) {
        if(attach.isRead) {
            attach.buffer.flip();
            byte    bytes[] = new byte[attach.buffer.limit()];
            attach.buffer.get(bytes);
            Charset cs = Charset.forName("UTF-8");
            String  message = new String(bytes, cs);
            //System.out.format("Server responded: " + message);
            if(message.length() > 0) {
                // get the ID from the charBuffer written to the channel
                if(message.charAt(1) == 'I') {
                    //System.out.println(message);
                    String      messageID = message.replaceAll("[^0-9]", "");
                    Integer     id = Integer.parseInt(messageID);
                    attach.ID = id;
                    attach.isRead = true;
                    attach.buffer.clear();
                    attach.channel.read(attach.buffer, attach, this);
                } else {
                    // split the message into fix message data
                    String[]    messageData = message.split("\\|");

                    // get market data and return fix message
                    // get broker ID
                    Integer     brokerID = Integer.parseInt(messageData[1]);

                    // get BUY / SELL
                    String      action = messageData[2];

                    // get Instrument symbol
                    String      instrument = messageData[3].toLowerCase();

                    // get Price
                    Integer     price = Integer.parseInt(messageData[4]);

                    // get Quantity
                    Integer     quantity = Integer.parseInt(messageData[5]);

                    // get status from API data analysis
                    String      status = "Rejected";

                    // API data based on symbol
                    try {
                        // get API data from json string
                        String apiData = getMarketData(instrument);
                        if(apiData.length() > 1) {
                            //System.out.println(apiData);
                            JSONObject  json = new JSONObject(apiData);
                            // Price High
                            Number      mPriceHigh = json.getNumber("week52High");
                            // Price Low
                            Number      mPriceLow = json.getNumber("week52Low");
                            // volume available (get % of current volume)
                            Integer     mVolume = (json.getNumber("latestVolume").intValue())/100;

                            // Check if transaction is valid
                            if(price < mPriceHigh.intValue() && price > mPriceLow.intValue()) {
                                // if sell... then add to sales available??
                                if((action.equals("BUY") && quantity <= mVolume) || (action.equals("SELL"))) {
                                    status = "Executed";
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.out.println("API call rejected...");
                    }

                    System.out.println("Broker message: " + message + "\n");

                    // Construct message
                    String  marketMessage = "";
                    marketMessage += brokerID + "|" + attach.ID + "|";
                    marketMessage += status + "|";
                    marketMessage += action + "|";
                    marketMessage += instrument + "|";
                    marketMessage += price + "|";
                    marketMessage += quantity + "|";

                    // calculate checksum
                    int     checksum = 0;
                    for(int i = 0; i < marketMessage.length(); i++) {
                        checksum += marketMessage.charAt(i);
                    }

                    marketMessage += Integer.toString(checksum);

                    // Send message
                    message = marketMessage;
                    System.out.println("Response: " + status);
                    attach.buffer.clear();
                    byte[] data = message.getBytes();
                    attach.buffer.put(data);
                    attach.buffer.flip();
                    attach.isRead = false;
                    attach.channel.write(attach.buffer, attach, this);
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

    // Alpha Vantage free API, market data and json object return
    private static String       getMarketData(String symbol) throws Exception {
        // get symbol your would like to get data for...
        // json returns high and low cost for the day, also volume...
        // IEX Trading API
        String      URLstring = "https://api.iextrading.com/1.0/stock/" + symbol + "/quote";

        URL         UrlObj = new URL(URLstring);
        HttpURLConnection   con = (HttpURLConnection) UrlObj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        // check response
        if(con.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code: " + con.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String      output;
        //System.out.println("Output from server.....");
        StringBuffer    content = new StringBuffer();
        while ((output = in.readLine()) != null) {
            content.append(output);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }
}

// Market hold business logic
// Receives messages from Broker...
// Replies with... Executed | Rejected