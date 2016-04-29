package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */
import java.net.*;
import java.io.*;

public class ChatClientThread extends Thread
{
    private Socket           socket   = null;
    private ChatClient       client   = null;
    private DataInputStream  streamIn = null;

    public ChatClientThread(ChatClient _client, Socket _socket) {
        client   = _client;
        socket   = _socket;
        open();
        start();
    }

    public void open() {
        try {
            streamIn  = new DataInputStream(socket.getInputStream());
        } catch(IOException e) {
            System.out.println("Error opening client input stream: " + e);
            client.stop();
        }
    }

    public void close() {
        try {
            if (streamIn != null) streamIn.close();
        } catch(IOException e) {
            System.out.println("Error closing client: " + e);
        }
    }

    public void run() {
        while (true) {
            try {
                System.out.println("waiting ... do input");
                client.handle(streamIn.readUTF());
            } catch(IOException e) {
                System.out.println("Error: " + e.getMessage());
                client.stop();
            }
        }
    }
}

