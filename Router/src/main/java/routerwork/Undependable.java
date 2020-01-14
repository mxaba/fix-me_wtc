package main.java.routerwork;

public interface Undependable
{
     int CHECKSUM = 1;
     int DISPATCH = 2;
     int ECHOBACK = 3;
     void performAction(Attachment attach, int resp);
}