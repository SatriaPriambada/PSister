package chatServer;

import org.json.JSONArray;
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
    private int counter = 0;

    private Player currentPlayer;
    private Player[] players = new Player [ChatServer.PLAYER_SIZE];
    private String IPReturn;
    private int portReturn;
    private UDPTransmitter udpTransmitter;
    private boolean accept = false;
    private boolean acceptLeader = false;
    private boolean finishElection = false;
    private int currentLeader = Player.ID_NOT_SET;
    private int previousLeader = Player.ID_NOT_SET;
    private String Time = "day";
    private int countVote = 0;

    public int getCurrentLeader(){
        return currentLeader;
    }

    public int getPreviousLeader(){
        return previousLeader;
    }

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
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                ListenSocket.receive(receivePacket);
                currentPlayer = client.getCurrentPlayer();
                players = client.getPlayers();

                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("RECEIVED from client: " + receivePacket.getSocketAddress() + sentence);
                IPReturn = receivePacket.getAddress().toString().substring(1);
                portReturn = receivePacket.getPort() - 1000;
                //System.out.println("IF FOUND RETURN TO " + IPReturn + ":" + portReturn);
                JSONObject jsonObject = new JSONObject(sentence);
                if (jsonObject.has("method")) {
                    //handle method this is 1st time request
                    String method = jsonObject.getString("method");
                    if (method.equals("prepare_proposal")) {
                        prepareProposalResponse();
                    } else if (method.equals("accept_proposal")) {
                        acceptProposalResponse(jsonObject.getInt("kpu_id"));
                    } else if (method.equals("vote_werewolf")) {
                        voteWerewolfResponse();
                    } else if (method.equals("vote_civillian")) {
                        voteCivilianResponse();
                    } else {
                        System.out.println(method + "does not exist");
                    }
                } else if (jsonObject.has("status")){
                    //handle object with status this is response from other client
                    if(jsonObject.getString("status").equalsIgnoreCase("ok")){
                        if(jsonObject.has("description")){
                            if(jsonObject.getString("description").equalsIgnoreCase("accepted")){
                                //count how many accepted if majority proceed with accept proposal
                                counter++;
                                if (counter > client.getNumberPlayer() / 2){
                                    int leaderCandidate = currentPlayer.getId();
                                    //when there is no accepted leader start the accept protocol
                                    if(!acceptLeader) {
                                        System.out.println("candidate : " + leaderCandidate);
                                        accept = false;
                                        counter = 0;
                                        client.acceptProposal(leaderCandidate);
                                        acceptLeader = true;
                                    } else {
                                        System.out.println("LEADER IS SELECTED : " + leaderCandidate);
                                        currentPlayer.setStatusPaxos("leader");
                                        //first selection
                                        if (currentLeader == Player.ID_NOT_SET) {
                                            currentLeader = leaderCandidate;
                                        } else {
                                            previousLeader = currentLeader;
                                            currentLeader = leaderCandidate;
                                        }
                                        finishElection = true;

                                        for(int i = 0; i < client.getNumberPlayer(); i++) {
                                            players[i].setFinish(finishElection);
                                            System.out.println("Set client " + i + " to " + finishElection);
                                            System.out.println(players[i]);
                                        }

                                        System.out.println("ALL FINISHED");
                                        client.KPUSelected(currentLeader);
                                    }
                                } else {
                                    accept = false;
                                }
                            } else if (jsonObject.getString("description").equalsIgnoreCase("")) {
                                if (Time.equals("day") && countVote == client.getNumberPlayer()){
                                    client.voteResultCivilian();
                                    countVote = 0;
                                } else if (Time.equals("night") && countVote == client.getNumberPlayer()){
                                    client.voteResultWerewolf();
                                    countVote = 0;
                                }
                            } else {
                                System.out.println(jsonObject);
                            }
                        } else {
                            System.out.println(jsonObject);
                        }
                    } else {
                        System.out.println(jsonObject);
                    }

                }
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
        JSONObject jsonObject = new JSONObject();

        //each node can only reply once
        if(!accept) {
            jsonObject.put("status", "ok");
            jsonObject.put("description", "accepted");
            if (currentPlayer.getStatusPaxos().equals("proposer")) {
                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
                System.out.println("I am acceptor");
                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            }
            accept = true;
        }  else {
            System.out.println("Already accepted other proposal");
            jsonObject.put("status", "failed");
            jsonObject.put("description","rejected");

            udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
            udpTransmitter.reply(jsonObject.toString());
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposalResponse(int candidateLeader) throws JSONException, InterruptedException {
        JSONObject jsonObject = new JSONObject();
        if(!accept) {
            jsonObject.put("status", "ok");
            jsonObject.put("description", "accepted");
            if (currentPlayer.getStatusPaxos().equals("proposer")) {
//                System.out.println("I am accept proposer");

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());

            } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
//                System.out.println("I am accept acceptor");

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            }
        }else {
            System.out.println("Already accepted other accept proposal");
            jsonObject.put("status", "failed");
            jsonObject.put("description","rejected");

            udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
            udpTransmitter.reply(jsonObject.toString());
        }


        //wait for signal
        //System.out.println("My ID "+currentPlayer.getId() + "candidate " + candidateLeader);
        if(currentPlayer.getId() != candidateLeader) {
            System.out.println("ALL FINISHED");
            client.KPUSelected(candidateLeader);
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolfResponse(){
        if (currentPlayer.getStatusPaxos().equals("leader")) {
            boolean once = true;
            System.out.println("I am KPU leader");
            JSONObject jsonObject = new JSONObject();
            Time = "night";

            try {
                //each node can only vote once
                if(once) {
                    jsonObject.put("status", "ok");
                    jsonObject.put("description", "");
                    once = false;
                    countVote++;
                } else {
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "");
                }

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilianResponse(){
        if (currentPlayer.getStatusPaxos().equals("leader")) {
            boolean once = true;
            System.out.println("I am KPU leader");
            JSONObject jsonObject = new JSONObject();
            Time = "day";

            try {
                //each node can only vote once
                if(once) {
                    jsonObject.put("status", "ok");
                    jsonObject.put("description", "");
                    once = false;
                    countVote++;
                } else {
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "");
                }

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
