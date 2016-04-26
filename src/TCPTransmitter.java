/**
 * Created by Satria on 4/26/2016.
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPTransmitter implements Runnable {
//    public static void main(String[] args) throws IOException {
//        int port=8888;
//        String IPReceived;
//
//        Scanner in= new Scanner(System.in);
//        IPReceived = in.nextLine();
//        port = in.nextInt();
//
//
//        TCPTransmitter transmitter = new TCPTransmitter();
//        transmitter.run(IPReceived, port);
//
//    }

    public TCPTransmitter(String _IPReceived, int _port) throws IOException {
        IPReceived = _IPReceived;
        port = _port;
        t = new Thread(this);
        t.setName(_IPReceived + ":" + port);
        t.start();
        //System.out.println("Transmitter" + _IPReceived + ":" + port);

    }

    @Override
    public void run() {
        Socket socket1 = null;
        PrintStream PS = null;
        String message;
        try {
            socket1 = new Socket(IPReceived,port);
            PS = new PrintStream(socket1.getOutputStream());
            Scanner in = new Scanner(System.in);
            while(true){
                String transMessage = in.nextLine();
                PS.println(transMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String IPReceived;
    private int port;
    private Thread t;
}
