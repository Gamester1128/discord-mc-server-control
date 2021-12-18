package testing;

import java.util.Scanner;

public class FakeMCServer {

    private static final int sleep = 2000;

    private static Runnable inputThread = () -> {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            System.out.println("[CANT BE ASKED FOR TIME] [FAKEMINECRAFTSERVER/ERROR] : no commands exist! input sent: " + input);
        }
    };

    public static void main(String[] args) {
        new Thread(inputThread, "FakeServer Input Thread").start();

        int i = 0;
        while(true) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("[69:69:69] [FAKEMINECRAFT/INFO] : Hehe counting hehe, now it is " + i++);
        }
    }

}