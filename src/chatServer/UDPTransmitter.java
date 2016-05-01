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
    private int myport;
    private DatagramSocket datagramSocket;
    private UnreliableSender unreliableSender;
    private boolean once = true;

    InetAddress IPAddress;

    public UDPTransmitter(ChatClient _client, String _IPTarget, int _portTarget, int _myport) {
        super();
        client = _client;
        IPTarget = _IPTarget;
        portTarget = _portTarget;
        myport = _myport;

        //start();
    }
    public void send(String message) throws InterruptedException {

        try {

            IPAddress = InetAddress.getByName(IPTarget);
            DatagramSocket datagramSocket = new DatagramSocket(myport + 1000);
            UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);
            String sentence = message;
            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portTarget);
            unreliableSender.send(sendPacket);
            datagramSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            Thread.sleep(10);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void reply(String message) throws InterruptedException {

        try {

            IPAddress = InetAddress.getByName(IPTarget);
            DatagramSocket datagramSocket = new DatagramSocket();
            UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);
            String sentence = message;
            byte[] sendData = sentence.getBytes();
            System.out.println("Reply :" + message + datagramSocket.getLocalSocketAddress());
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portTarget);
            datagramSocket.send(sendPacket);
            datagramSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            Thread.sleep(10);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
