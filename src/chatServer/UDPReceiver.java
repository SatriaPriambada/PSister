package chatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

public class UDPReceiver extends Thread
{
	/**
	 * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
	 * balasan juga dikirim melalui Unreliable Sender.
	 */


	private ChatClient       client    = null;
	private Socket socket    = null;
	private int              ID        = -1;
	private DataInputStream streamIn  =  null;
	private DataOutputStream streamOut = null;

	public UDPReceiver(ChatClient _client, Socket _socket) {
		super();
		client = _client;
		socket = _socket;
		ID     = socket.getLocalPort();
		start();
	}

	public void run()
	{
		int listenPort = ID;
		try {
			DatagramSocket ListenSocket = new DatagramSocket(listenPort);
			byte[] receiveData = new byte[1024];
			while(true)
			{
				System.out.println("waiting ...");
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				ListenSocket.receive(receivePacket);

				String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
				System.out.println("RECEIVED from client: " + sentence);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
}
