/**
 * Created by Satria on 4/26/2016.
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {
        System.out.println("Main Thread started");
        int port=8888;
        String IPReceived;

        Scanner in= new Scanner(System.in);
        IPReceived = in.nextLine();
        port = in.nextInt();
        TCPReceiver recv = new TCPReceiver(IPReceived,port);
        //recv.run();
        TCPTransmitter trans = new TCPTransmitter(IPReceived,port);
        //trans.run();
        System.out.println("Main Thread finished");
    }
}
