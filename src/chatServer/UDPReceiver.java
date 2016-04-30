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
            currentPlayer = client.getCurrentPlayer();
            players = client.getPlayers();

			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			System.out.println("RECEIVED from client: " + sentence);
            String IPTarget = receivePacket.getAddress().toString().substring(1);
            System.out.println("Send" + IPTarget + ":" + receivePacket.getPort());
            UDPTransmitter udpTransmitter = new UDPTransmitter(client, IPTarget, receivePacket.getPort() - 1000, ID);
            udpTransmitter.send("REPLY HELLO");
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    /*-------------------------- Method Prepare Proposal Paxos---------------------------*/
    void prepareProposalResponse() throws JSONException, InterruptedException {
        if(currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
            System.out.println("I am proposer");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method","prepare_proposal");
            int numberPlayer = client.getNumberPlayer();
            int proposalID = client.getProposalID();
            jsonObject.put("proposal_id", "["+ proposalID + ","+ currentPlayer.getId() + "]");
            for (int i = 0; i < numberPlayer; i++ ){
                System.out.println("loop :" + players[i].getAddrIp() + " : "+ players[i].getAddrPort());
                UDPTransmitter transmitterUDP = new UDPTransmitter(client, players[i].getAddrIp(), players[i].getAddrPort(), socket.getLocalPort());
                transmitterUDP.send(jsonObject.toString());
            }
        } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposalResponse(){
        if(currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolfResponse(){
        if(currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilianResponse(){
        if(currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }
}
