import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    
    static Socket socket;
    public static void main(String[] args) {
        final ConsoleProcess cp = new ConsoleProcess("cd fakeserver;java -jar fakeserver.jar");
        cp.start();
        String line = null;

        try {
            socket = new Socket("127.0.0.1", 5000);
            System.out.println("connected");
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                cp.writeLine(scanner.nextLine());
            }
        }).start();

        while ((line = cp.readLine()) != null) {
            System.out.println(line);
        }

    }

}
