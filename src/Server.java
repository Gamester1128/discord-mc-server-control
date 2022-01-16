import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable {

    private boolean rawPackets = true;
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
    private boolean saveCpOut = true;
    private boolean serverStarting = false;
    private boolean serverStopping = false;

    private static final String PREFIX_PING = "/i/";
    private static final String PREFIX_DISCONNECT = "/d/";
    private static final String PREFIX_CONNECT = "/c/";
    private static final String PREFIX_OUTPUT = "/o/";
    private static final String PREFIX_OUTPUT_START = "/s/";
    private static final String PREFIX_OUTPUT_END = "/e/";
    private static final String PREFIX_MESSAGE = "/m/";

    private Object readCPLock = new Object();

    public Server(int port) {
        cp = new ConsoleProcess("cd fakeserver && java -jar fakeserver.jar");
        // cp = new ConsoleProcess("run2.bat", "C:\\mc server\\vanilla\\1.18prerel5");

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook, "shutdown hook"));

        // Receiving
        new Thread(this::receiveFromClients, "Receiving Thread").start();

        // manage connection with client
        new Thread(this::manageClients, "Manage Connection").start();

        // read stdout of cp
        new Thread(this::readAllCP, "Read all stdout from cp").start();

        // input into stdin of cp
        Scanner scanner = new Scanner(System.in);
        String input;
        while (running) {
            input = scanner.nextLine();
            processStdinToCP(input);
        }
        scanner.close();
    }

    private void processStdinToCP(String input) {
        if (input.startsWith("!"))
            switch (input) {
                case "!start" -> startCP();
                default -> System.out.println("ERROR : Unknown command, input=" + input);
            }
        else {
            if (cp.started())
                cp.writeLine(input);
        }
    }

    private void startCP() {
        if (!cp.started()) {
            cp.start();
            synchronized (readCPLock) {
                readCPLock.notify();
            }
        } else if (serverStarting) {
            System.out.println("ERROR - Attempting to start server when still starting");
        } else {
            System.out.println("ERROR - Attempting to start server when it is already started");
        }
    }

    private void stopCP() {
        if (cp.started())
            cp.writeLine("stop");
    }

    public void shutdownHook() {
        if (cp.started()) {
            System.out.println("shutting down lel pussy");
            stopCP();
        }
    }

    public void manageClients() {
        synchronized (this) {
            while (running) {
                if (discordBot == null)
                    try {
                        System.out.println("NOTICE - waiting to manage client...");
                        wait();
                        System.out.println("NOTICE - managing client...");
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
        while (running) {
            line = cp.readLine();
            if (line == null) {
                synchronized (readCPLock) {
                    try {
                        System.out.println("NOTICE - Waiting for cp to start...");
                        readCPLock.wait();
                        System.out.println("NOTICE - Reading stdout of cp...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (line == null)
                continue;
            line = filterMinecraftOutput(line);
            if (!saveCpOut)
                return;
            if (printCP)
                System.out.println(line);

            // filter
            if (line != "")
                cpOuts.add(line);

            if (line.contains("Done (")) {
                saveCpOut = true;
                serverStarting = false;
                flush();
                sleep(300);
                send(discordBot,
                        "/m/@everyone BIRB ALERT https://tenor.com/view/space-laces-vaultage-bird-alert-vaultage003-gif-20725836");
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

        } else if (data.startsWith(PREFIX_MESSAGE)) {
            String message = data.split("/m/|/e/")[1];
            processStdinToCP(message);
            sleep(300);
            flush();
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
        if (messages.length() == 0)
            return;

        int numOfMessages = messages.length() / MAX_DATA_SIZE;
        if (messages.length() % MAX_DATA_SIZE != 0)
            numOfMessages++;
        System.out.println("------------------------length of messages: " + messages.length());
        // send number of /o/ packets containing output sliced
        send(discordBot, PREFIX_OUTPUT_START + numOfMessages);

        for (int i = 0; i < numOfMessages; i++) {
            int beginIndex = MAX_DATA_SIZE * i;
            int endIndex = (MAX_DATA_SIZE * (i + 1) <= messages.length()) ? MAX_DATA_SIZE * (i + 1) : messages.length();
            String sendString = messages.substring(beginIndex, endIndex);
            send(discordBot, PREFIX_OUTPUT + sendString);
            System.out.println(i + ": length " + sendString.length());
        }

        // indicate done sending sliced packets
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
