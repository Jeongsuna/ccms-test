package org.example;

public class TT {

    private static final ServerList serverList = new ServerList( new SocketStub() );

    public static void main(String[] args) throws InterruptedException {
        // Job 도는 부분이라고 생각하면 될듯

        long consumeTime = 0;
        while ( true ){
            long sleepTime = 1000;
            Thread.sleep( sleepTime ); // 1초마다 한번씩
            consumeTime += sleepTime;
            System.out.println( ((int)(consumeTime / 1000)) + "초");
            serverList.roundCheckAndSendNotice();
        }
    }

}
