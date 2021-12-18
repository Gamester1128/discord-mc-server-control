import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import javax.swing.event.SwingPropertyChangeSupport;

public class ConsoleProcess {

    private static final String serverpath = "fakeserver/fakeserver.jar";

    public static void main(String[] args) {
        
        final Process p;
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "run2.bat").redirectErrorStream(true);
        //pb.directory(new File("C:\\Dev\\git\\discord-mc-server-control\\fakeserver"));
        pb.directory(new File("C:\\mc server\\vanilla\\1.18 prerel5"));
        try {
            p = pb.start();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(pb.directory());
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            //System.out.println("Waiting my queen");
            //new Scanner(System.in).nextLine();

            String line = null;

            new Thread(()->{
                BufferedWriter scan = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                Scanner scanner = new Scanner(System.in);

                while (true){
                    String next = scanner.nextLine();
                    try {
                        System.out.println("passed: " + next);
                        scan.write(next);
                        scan.write("\n");
                        scan.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
            }).start();

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            System.out.println("CLosing...");
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

}