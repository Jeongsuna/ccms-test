package org.example;

import java.util.ArrayList;
import java.util.Date;

// 커넥션 테스트기
interface Socket{
    boolean isDownServer();
}
class SocketStub implements Socket{

    @Override
    public boolean isDownServer() {
        return true;
    }
}

class ConnectionNotice{

    private Date lastNotice;
    private final long cycle; // 알림발송주기
    private final String serverName;
    private boolean serverStatus;

    public static final long ONE_SEC = 1000;
    public static final long ONE_MINUTE = ONE_SEC * 60;
    public static final long ONE_HOUR = ONE_MINUTE * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;

    public ConnectionNotice( String serverName, long cycle) {
        this.serverName = serverName;
        this.cycle = cycle;
        this.serverStatus = false;
    }
    public ConnectionNotice( String serverName ){
        this( serverName, ONE_DAY );
    }

    public void send(){
        System.out.println( "["+serverName+"]" + ": 알림발송" );
        lastNotice = new Date(); // 마지막 알림 시간 갱신
    }

    public void checkConnection( Socket socket ){
        if( socket.isDownServer() && ( lastNotice == null /* 처음 시작이면 null */ || (lastNotice.getTime() + cycle) < new Date().getTime() ) ){
            send();
            serverStatus = false;
        }
        else{
            serverStatus = true;
        }
    }
}

class ServerList extends ArrayList<ConnectionNotice> {

    private final Socket socket;

    public ServerList( Socket socket ){
        this.socket = socket;

        // 기본 서버 설정
        this.add( new ConnectionNotice("서버1", ConnectionNotice.ONE_MINUTE * 2) ); // 2분주기 알람 설정
        this.add( new ConnectionNotice("서버2", ConnectionNotice.ONE_MINUTE)); // 1분주기 알람설정
        this.add( new ConnectionNotice("서버3", ConnectionNotice.ONE_SEC * 30) ); // 30초 주기 알람설정
        this.add( new ConnectionNotice("서버4", ConnectionNotice.ONE_SEC * 10) ); // 10초 주기 알람 설정
        this.add( new ConnectionNotice("서버4", ConnectionNotice.ONE_SEC * 10) ); // 10초 주기 알람 설정
        this.add( new ConnectionNotice("서버4", ConnectionNotice.ONE_SEC * 10) ); // 10초 주기 알람 설정
    }

    public void roundCheckAndSendNotice(){
        this.forEach( cn -> cn.checkConnection( socket ) );
    }

}

public class TTT {

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
