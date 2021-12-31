import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable {

    private boolean rawPackets = false;
    private boolean printCP = true;

    public static final int MAX_ATTEMPTS = 3;
    private static final int MAX_DATA_SIZE = 500;

    private final ConsoleProcess cp;
    private DatagramSocket socket;
    private boolean running = false;
    private ArrayList<String> cpOuts = new ArrayList<String>();
    private static volatile ServerClient discordBot;

    private Thread senderThread;
    private int currSenderThreadNum = 0;
    private int selectedSenderThreadNum = 0;

    private boolean disconnected = true;
    private boolean saveCpOut = false;

    private static final String PREFIX_PING = "/i/";
    private static final String PREFIX_DISCONNECT = "/d/";
    private static final String PREFIX_CONNECT = "/c/";
    private static final String PREFIX_OUTPUT = "/o/";
    private static final String PREFIX_OUTPUT_START = "/s/";
    private static final String PREFIX_OUTPUT_END = "/e/";
    private static final String PREFIX_MESSAGE = "/m/";

    public Server(int port) {
        // cp = new ConsoleProcess("cd fakeserver && java -jar fakeserver.jar");
        cp = new ConsoleProcess("run2.bat", "C:\\mc server\\vanilla\\1.18prerel5");

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        cp.start();
        running = true;

        // Receiving
        new Thread(this::receiveFromClients, "Receiving Thread").start();

        // manage connection with client
        new Thread(this::manageClients, "Manage Connection").start();

        // read stdout of cp
        new Thread(this::readAllCP, "Read all stdout from cp").start();

        // input into stdin of cp
        Scanner scanner = new Scanner(System.in);
        while (running) {
            cp.writeLine(scanner.nextLine());
        }
        scanner.close();
    }

    private void processStdinToCP(String input) {
        switch (input) {
            case "start" -> cp.start();
            case "end" -> cp.end();
            default -> cp.writeLine(input);
        }
    }

    public void manageClients() {
        synchronized (this) {
            while (running) {
                if (discordBot == null)
                    try {
                        System.out.println("now waiting...");
                        wait();
                        System.out.println("now not waiting");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                send(discordBot, PREFIX_PING);

                sleep(2 * 1000);

                if (discordBot == null)
                    continue;

                if (discordBot.attempts > 0)
                    discordBot.attempts--;
                else
                    disconnect();
            }
        }
    }

    public void sendToClients() {
        int senderNum = ++currSenderThreadNum;
        while (selectedSenderThreadNum == senderNum) {
            sleep(10 * 1000);
            flush();
        }
    }

    public void receiveFromClients() {
        while (running) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            processReceivedPackets(packet);
        }
    }

    public void readAllCP() {

        String line;
        while ((line = cp.readLine()) != null) {
            // only start saving
            if (line.contains("Done (")) {
                saveCpOut = true;
            }
            if (saveCpOut) {
                if (printCP)
                    System.out.println(line);
                cpOuts.add(line);
            }
        }
    }

    private void processReceivedPackets(DatagramPacket packet) {
        String data = new String(packet.getData());
        if (rawPackets)
            System.out.println(new ServerClient(packet.getAddress(), packet.getPort()) + "R:" + data);

        if (data.startsWith(PREFIX_CONNECT)) {
            if (discordBot != null) {
                System.out.println("WARNING, TWOOOO Discord Bots are trying to connect, rejecting former!");
                return;
            }
            synchronized (this) {
                discordBot = new ServerClient(packet.getAddress(), packet.getPort());
                System.out.println("Succesfully connected client: " + discordBot);
                senderThread = new Thread(this::sendToClients, "Sender Thread");
                senderThread.start();
                selectedSenderThreadNum++;
                notify();
            }

        } else if (data.startsWith("/m/")) {
            System.out.println("message: " + data.split("/m/|/e/")[1]);

        } else if (data.startsWith(PREFIX_DISCONNECT)) {
            System.out.println("Successfully disconnected discordBot: " + discordBot);
        } else if (data.startsWith(PREFIX_PING)) {
            if (discordBot == null)
                return;
            discordBot.attempts = MAX_ATTEMPTS;
        }
    }

    public void flush() {
        String messages = "";
        for (int i = 0; i < cpOuts.size(); i++) {
            messages += cpOuts.get(i) + "\n";
        }

        // nothing to send
        if (messages == "")
            return;

        int numOfMessages = messages.length() / MAX_DATA_SIZE;
        if (numOfMessages % MAX_DATA_SIZE == 0)
            numOfMessages++;

        send(discordBot, PREFIX_OUTPUT_START + numOfMessages);

        for (int i = 0; i < numOfMessages; i++) {
            int beginIndex = MAX_DATA_SIZE * i;
            int endIndex = (MAX_DATA_SIZE * (i + 1) <= messages.length()) ? MAX_DATA_SIZE * (i + 1) : messages.length();
            send(discordBot, PREFIX_OUTPUT + messages.substring(beginIndex, endIndex));
        }

        send(discordBot, PREFIX_OUTPUT_END);

        cpOuts.clear();
    }

    public String filterMinecraftOutput(String line) {
        if (line == null)
            return "";
        if (line.toLowerCase().contains("exception"))
            return line.substring(0, line.toLowerCase().indexOf("exception"));

        return line;
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

    public void disconnect() {
        selectedSenderThreadNum++;
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
