/**
 * Created by Satria on 4/26/2016.
 */
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {
        System.out.println("Main Thread started");
        int port=5678;
        String IPReceived = "127.0.0.1";
//        Used for initialization in comment for easiser development
//        Scanner in= new Scanner(System.in);
//        IPReceived = in.nextLine();
//        port = in.nextInt();
        int backlog = 50;
        boolean waitReady = true;

        try {
            InetAddress addr = InetAddress.getByName(IPReceived);
            ServerSocket serverSocket = new ServerSocket(port, backlog, addr);
            Socket s = serverSocket.accept();
            sockets.add(s);

            InputStreamReader IR = new InputStreamReader(sockets.get(0).getInputStream());
            BufferedReader BR = new BufferedReader(IR);

            while(waitReady) {

                String message = BR.readLine();
                System.out.println(message);
                if (!message.equals("start")) {
                    System.out.println("Game Ready lets go!");
                    waitReady = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //recv.run();
//        TCPTransmitter trans = new TCPTransmitter(IPReceived,port);
        //trans.run();
        System.out.println("Main Thread finished");
    }

    private static List<Socket> sockets;
}
