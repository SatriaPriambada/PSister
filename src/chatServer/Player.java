package chatServer;

/**
 * Created by acel on 29-Apr-16.
 */
public class Player {
    public static final int ALIVE = 1;
    public static final int DEAD = 0;
    public static final int ID_NOT_SET = -99;

    private int addrPort;
    private String addrIp;
    private int id;
    private String username;
    private int alive;
    private String statusPaxos;
    private String rolePlayer;
    private boolean isFinish;
    private boolean isReady;

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public Player(){
        /* do nothing */
    }
    public Player(String addrIp, int addrPort){
        this.addrPort = addrPort;
        this.addrIp = addrIp;
        this.id = ID_NOT_SET;
        this.username = "";
        alive = ALIVE;
        isFinish = false;
    }

    public int getAlive() {
        return alive;
    }

    public void setAlive(int alive) {
        this.alive = alive;
    }

    public int getAddrPort() {
        return addrPort;
    }

    public void setAddrPort(int addrPort) {
        this.addrPort = addrPort;
    }

    public String getAddrIp() {
        return addrIp;
    }

    public void setAddrIp(String addrIp) {
        this.addrIp = addrIp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRolePlayer() {
        return rolePlayer;
    }

    public void setRolePlayer(String RolePlayer) {
        this.rolePlayer = RolePlayer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatusPaxos() {
        return statusPaxos;
    }

    public void setStatusPaxos(String StatusPaxos) {
        this.statusPaxos = StatusPaxos;
    }

    public String toString(){
        return ("player_id " + id +
                " is_alive " + alive +
                " address " + addrIp +
                " port " + addrPort +
                " username " + username +
                " isFinish " + isFinish
        );
    }

    public void setFinish(boolean fin) { isFinish = fin; }

    public boolean getFinish(){ return isFinish; }
}
