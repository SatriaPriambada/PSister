package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */
import java.io.*;
import java.net.*;

public class UDPTransmitter extends Thread{
    /**
     * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
     * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
     */

    private ChatClient       client    = null;
    private String IPTarget;
    private int portTarget;

    public UDPTransmitter(ChatClient _client, String _IPTarget, int _portTarget) {
        super();
        client = _client;
        IPTarget = _IPTarget;
        portTarget = _portTarget;
        //start();
    }
    public void send(String message)
    {
        InetAddress IPAddress = null;

        try {
            IPAddress = InetAddress.getByName(IPTarget);

            DatagramSocket datagramSocket = new DatagramSocket();
            UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);

            String sentence = message;

            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portTarget);
            unreliableSender.send(sendPacket);
            datagramSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
