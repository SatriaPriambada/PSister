package chatServer;

/**
 * Created by Satria on 5/13/2016 with name chatServer
 */
public class WaitingThread extends Thread {
    private UDPReceiver udpReceiver;
    private ChatClient client;
    private long milis;

    public WaitingThread(UDPReceiver _udpReceiver, ChatClient _client){
        udpReceiver = _udpReceiver;
        client = _client;
    }
    public void run(){
        milis = System.currentTimeMillis();
        while (udpReceiver.counter < client.getCurrentlyPlaying() && milis < 1000) {
            milis = System.currentTimeMillis() - milis;
        }

    }

}
