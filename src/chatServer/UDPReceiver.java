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
    private int proposalNumber;
    private int currentLeader;
    private int previousLeader;
    private Player currentPlayer;
    private Player[] players = new Player [ChatServer.PLAYER_SIZE];

	public UDPReceiver(ChatClient _client, Socket _socket) {
		super();
		client = _client;
		socket = _socket;
		ID     = socket.getLocalPort();
        String localIP = socket.getLocalAddress().getHostAddress();
        currentPlayer = new Player(localIP, ID);
        proposalNumber = 0;
        currentLeader = Player.ID_NOT_SET;
        previousLeader = Player.ID_NOT_SET;
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
            InetAddress IPSender = receivePacket.getAddress();
            int portSender = receivePacket.getPort();

			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("RECEIVED from client: " + IPSender + ":" + portSender + "receive" + sentence);

            JSONObject jsonObject = new JSONObject(sentence);
            String method = jsonObject.getString("method");
            if (method.equals("prepare_proposal")) {
                prepareProposalResponse();
            } else if (method.equals("accept_proposal")) {
                acceptProposalResponse();
            } else if (method.equals("vote_werewolf")) {
                voteWerewolfResponse();
            } else if (method.equals("vote_civillian")) {
                voteCivilianResponse();
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
    void prepareProposalResponse(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer do nothing");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");

        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader do nothing");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposalResponse(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolfResponse(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilianResponse(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }
}
