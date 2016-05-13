package chatServer;

import org.json.JSONException;

/**
 * Created by Satria on 5/13/2016 with name chatServer
 */
public class WaitingThread extends Thread {
    private UDPReceiver udpReceiver;
    private ChatClient client;
    private long milis;
    private long duration;

    public WaitingThread(UDPReceiver _udpReceiver, ChatClient _client){
        udpReceiver = _udpReceiver;
        client = _client;
        milis = System.currentTimeMillis();
    }
    public void run(){
        duration = System.currentTimeMillis() - milis;
        System.out.println("Waiting timeout " + udpReceiver.counter + " current player " + client.getCurrentlyPlaying() + " milis " + duration);

        while (udpReceiver.counter < client.getCurrentlyPlaying() && duration < 10000) {
            duration = System.currentTimeMillis() - milis;
        }


        System.out.println("Timeout finished " + udpReceiver.counter + " current player " + client.getCurrentlyPlaying() + " milis " + duration);
        try {
            if ( udpReceiver.counter <= client.getCurrentlyPlaying()/2){
                client.prepareProposal();

                udpReceiver.counter = 0;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
