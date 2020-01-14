package main.java.routerwork;

public class Main {
  
  public static void main(String[] args) {
    String host = "localhost";
    int port = 5000;
    Router router = new Router(host, port);
    try {
      router.startServers();
    }
    catch(Exception e) {
      System.out.println(e);
    }
  }
}