package testing;

import java.util.Scanner;

public class FakeMCServer {

    private static final int sleep = 100;

    private static Runnable inputThread = () -> {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if (input.equals("stop"))
                System.exit(0);
            System.out.println(
                    "[CANT BE ASKED FOR TIME] [FAKEMINECRAFTSERVER/ERROR] : no commands exist! input sent: " + input);
        }
    };

    public static void main(String[] args) {
        new Thread(inputThread, "FakeServer Input Thread").start();

        int i = 0;
        while (true) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (i == 5)
                System.out.println("[69:69:69] [FAKEMINECRAFT/INFO] : Done (7.27ms)! for help, type \"help\"");
            else
                System.out.println("[69:69:69] [FAKEMINECRAFT/INFO] : Hehe counting hehe, now it is " + i);
            i++;
        }
    }

}