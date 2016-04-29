import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;

/**
 * Created by Satria on 4/28/2016.
 */
public class Client {
    /**
     * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
     * balasan juga dikirim melalui .
     */
    public static void main(String[] args) throws IOException {
        System.out.println("Main Thread started");
        int port=5678;
        String IPReceived = "127.0.0.1";

        Scanner in= new Scanner(System.in);
//        IPReceived = in.nextLine();
//        port = in.nextInt();


        TCPTransmitter transmitter = new TCPTransmitter(IPReceived, port);
        System.out.println("Main Thread finished");

    }
}
