package main.java.broker;

public class Main {
  public static void main(String[] args) throws Exception {
    //args[0] == market id && args[1] == 1 == buy || args[1] == 2 == sell
    if (args.length == 2) {
      if (args[1].equals("1") || args[1].equals("2")) {
        Broker broker = new Broker(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        try {
          broker.contact();
        } catch (Exception e) {
          System.out.println(e);
        }
      } else {
        System.out.println("It's either buy or sell [buy = 1 or sell = 2]");
      }

    } else {
      System.out.println("USAGE: java -jar target/broker-1.0-SNAPSHOT.jar  market id(e.g 10000) 1 (buy) or 2 (sell)");
    }
  }
}