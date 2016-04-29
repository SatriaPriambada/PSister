/**
 * Created by Satria on 4/26/2016.
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPReceiver implements Runnable{
//    public static void main(String[] args) throws IOException {
//        int port=8888;
//        String IPReceived;
//
//        Scanner in= new Scanner(System.in);
//        IPReceived = in.nextLine();
//        port = in.nextInt();
//
//
//        TCPReceiver recv = new TCPReceiver();
//        recv.run(IPReceived, port);
//
//    }
    public TCPReceiver (String _IPReceived, int _port) throws IOException {
        IPReceived = _IPReceived;
        port = _port;
        t = new Thread(this);
        t.setName(_IPReceived + ":" + port);
        t.start();
        //System.out.println("Here" + _IPReceived + ":" + port);
    }

    private int backlog;

    @Override
    public void run() {
        backlog = 50;
        boolean waitReady = true;

        try {
            InetAddress addr = InetAddress.getByName(IPReceived);
            ServerSocket serverSocket = new ServerSocket(port, backlog, addr);

            sockets = serverSocket.accept();

            InputStreamReader IR = new InputStreamReader(sockets.getInputStream());
            BufferedReader BR = new BufferedReader(IR);

            while(waitReady) {

                String message = BR.readLine();
                if (message != null) {
                    System.out.println(message);
                    waitReady = false;
                }
            }
            while(true) {
                String message = BR.readLine();
                if (message != null) {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String IPReceived;
    private int port;
    private Thread t;
    public static Socket sockets;
}
