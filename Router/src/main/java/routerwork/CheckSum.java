package main.java.routerwork;

public class CheckSum implements Undependable
{
    private int CHECKSUM = Undependable.CHECKSUM;
    @Override
    public void performAction(Attachment attach, int resp) {
        if (resp != CHECKSUM) {
            new ForwardTo().performAction(attach, resp);
            return ;
        }

        int size = getMsgSize(attach.msg);
        int checksum = getCheckSum(attach.msg[attach.msg.length - 1]);
        int action = Undependable.ECHOBACK;
        if (size % 256 != checksum)
            action = Undependable.ECHOBACK;
        else
            action = Undependable.DISPATCH;
        new ForwardTo().performAction(attach, action);
    }

    private int getMsgSize(String datum[]) {
        int j = 0;
		char t[];

		for(int k = 0; k < datum.length - 1; k++) {
			t = datum[k].toCharArray();
			for(int i = 0; i < t.length; i++) {
				j += (int)t[i];
			}
			j += 1;
		}
        return (j);
    }

    private int getCheckSum(String part) {
        int tag, value;
        try {
            String ops[] = part.split("=");
            tag = Integer.parseInt(ops[0]);
            value = Integer.parseInt(ops[1]);
            if (tag == 10)
                return value;
            }
        catch(Exception e) {
        System.out.println("Error message passed");
        }
        return (0);
    }
}