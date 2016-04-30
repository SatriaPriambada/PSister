package chatServer;

import org.json.JSONException;
import org.json.JSONObject;

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

    private Player currentPlayer;
    private Player[] players = new Player [ChatServer.PLAYER_SIZE];

	public UDPReceiver(ChatClient _client, Socket _socket) {
		super();
		client = _client;
		socket = _socket;
		ID     = socket.getLocalPort();
        String localIP = socket.getLocalAddress().getHostAddress();
        currentPlayer = new Player(localIP, ID);
		start();
	}

	public void run()
	{
		int listenPort = ID;
		try {
			DatagramSocket ListenSocket = new DatagramSocket(listenPort);
			byte[] receiveData = new byte[1024];
			System.out.println("waiting ...");
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			ListenSocket.receive(receivePacket);


			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("RECEIVED from client: " + receivePacket.getAddress() + ":" + receivePacket.getPort() + sentence);

            JSONObject jsonObject = new JSONObject(sentence);
            String method = jsonObject.getString("method");
            if (method.equals("prepare_proposal")) {
                prepareProposal();
            } else if (method.equals("accept_proposal")) {
                acceptProposal();
            } else if (method.equals("vote_werewolf")) {
                voteWerewolf();
            } else if (method.equals("vote_civillian")) {
                voteCivilian();
            }


		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /*-------------------------- Method Prepare Proposal Paxos---------------------------*/
    void prepareProposal(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposal(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolf(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilian(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }
}
