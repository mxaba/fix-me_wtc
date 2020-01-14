package main.java.market;

import java.util.Scanner;

public class Main
{
  //args[0] == quantity, args[1] == price
  public static void main(String[] args) {
    if (args.length != 0) {
        Market market = new Market(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    try {
      market.contact();
    }
    catch(Exception e) {
      System.out.println(e);
    }
  } else {
      System.out.println( "ERROR \n" + "USAGE: java -jar target/market-1.0-SNAPSHOT.jar specify quantity AND specify the price");
    }
  }
}