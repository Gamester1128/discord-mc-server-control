import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class Server implements Runnable {

    private boolean rawPackets = true;
    private boolean printCP = false;

    public static final int MAX_ATTEMPTS = 3;
    private static final int MAX_DATA_SIZE = 500;

    private final ConsoleProcess cp;
    private DatagramSocket socket;
    private boolean running = false;
    private ArrayList<String> cpOuts = new ArrayList<String>();
    private volatile ServerClient discordBot;

    private Thread senderThread;
    private Runnable senderRunnable;
    private int currSenderThreadNum = 0;
    private int selectedSenderThreadNum = 0;

    private boolean disconnected = true;
    private ArrayList<ServerClient> clients = new ArrayList<>();

    public Server(int port) {
        cp = new ConsoleProcess("cd fakeserver && java -jar fakeserver.jar");

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        cp.start();
        running = true;
        // Sending
        senderRunnable = () -> {
            int senderNum = ++currSenderThreadNum;
            while (selectedSenderThreadNum == senderNum) {
                sleep(10 * 1000);
                flush();
            }
        };

        // Receiving
        new Thread(() -> {
            while (running) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);

                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                process(packet);
            }
        }, "Receiving Thread").start();

        // manage connection with client
        new Thread(() -> {
            while (running) {
                if (discordBot != null) {
                    send(discordBot, "/i/");
                    System.out.println(discordBot);
                }
                sleep(2 * 1000);
                if (discordBot == null)
                    continue;

                if (discordBot.attempts > 1)
                    discordBot.attempts--;
                else
                    disconnect();
            }
        }, "Manage Connection").start();

        String line;
        while ((line = cp.readLine()) != null) {
            if (printCP)
                System.out.println(line);
            cpOuts.add(line);
        }
    }

    private void process(DatagramPacket packet) {
        String data = new String(packet.getData());
        if (rawPackets)
            System.out.println(new ServerClient(packet.getAddress(), packet.getPort()) + "R:" + data);

        if (data.startsWith("/c/")) {
            if (discordBot != null) {
                System.out.println("WARNING, TWOOOO Discord Bots are trying to connect, rejecting former!");
                return;
            }
            discordBot = new ServerClient(packet.getAddress(), packet.getPort());
            System.out.println("Succesfully connected client: " + discordBot);
            senderThread = new Thread(senderRunnable, "Sender Thread");
            selectedSenderThreadNum++;

        } else if (data.startsWith("/m/")) {
            System.out.println("message: " + data.split("/m/|/e/")[1]);

        } else if (data.startsWith("/d/")) {
            System.out.println("Successfully disconnected discordBot: " + discordBot);

        } else if (data.startsWith("/i/")) {
            if (discordBot == null)
                return;
            discordBot.attempts = MAX_ATTEMPTS;
        }
    }

    public void flush() {
        String message = "";
        for (int i = 0; i < cpOuts.size(); i++) {
            message += cpOuts.get(i);
        }
        send(discordBot, "/m/" + message + "/e/");
        cpOuts.clear();
    }

    public void sendToAll(ArrayList<ServerClient> clients, String message) {
        for (ServerClient client : clients)
            send(client, message);
    }

    public void send(ServerClient client, String message) {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, client.ip, client.port);

        if (rawPackets)
            System.out.println(client + "S:" + message);

        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send to client!");
            e.printStackTrace();
        }
    }

    public synchronized void disconnect() {
        System.out.println("Successfully disconnected client: " + discordBot);
        discordBot = null;
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
