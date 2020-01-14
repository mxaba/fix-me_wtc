package main.java.routerwork;

public class EchoBack implements Undependable
{
    private int ECHOBACK = Undependable.ECHOBACK;
    @Override
    public void performAction(Attachment attatch, int resp)
    {
        if (resp != ECHOBACK)
            return ;
        attatch.isRead = false;
        attatch.client.write(attatch.buffer, attatch, attatch.rwHandler);
    }
}