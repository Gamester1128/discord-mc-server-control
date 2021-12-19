import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        final ConsoleProcess cp = new ConsoleProcess("cd fakeserver;java -jar fakeserver.jar");
        cp.start();
        String line = null;

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
