import java.net.InetAddress;

public class ServerClient {

    public InetAddress ip;
    public int port;
    public int attempts = Server.MAX_ATTEMPTS;
    public boolean responded = false;

    public ServerClient(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String toString() {
        return "[" + ip + "|" + port + "]";
    }

}
