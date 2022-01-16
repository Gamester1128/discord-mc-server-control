import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ConsoleProcess {

    private Process p;
    private ProcessBuilder pb;
    private BufferedWriter bw;
    private BufferedReader br;

    public ConsoleProcess(String commandToExecute, String workingDirectory) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.out.println(System.getProperty("os.name"));
            pb = new ProcessBuilder("cmd.exe", "/c", commandToExecute);
        } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            pb = new ProcessBuilder("/bin/bash", "-c", commandToExecute);
        } else {
            System.out.println(
                    "What niche OS you using there bud.. or how ignorant am I? OS: " + System.getProperty("os.name"));
            System.exit(1);
        }
        pb.directory(new File(workingDirectory));
        pb.redirectErrorStream(true);
    }

    public ConsoleProcess(String commandToExecute) {
        this(commandToExecute, System.getProperty("user.dir"));
    }

    public void start() {
        if (started()) {
            System.out.println("WARNING - ATTEMPTING TO START MULTIPLE CONSOLE PROCESSES");
            return;
        }
        try {
            System.out.println(pb.directory());
            p = pb.start();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                p.waitFor();
                br.close();
                bw.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("destroying process");
            p.destroyForcibly();
            p = null;
            Main.server.flush();
        }, "Cleans up when process is done").start();
    }

    @Deprecated
    public void stop() {

    }

    public String readLine() {
        if (br == null)
            return null;
        try {
            String line = br.readLine();
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeLine(String line) {
        if (bw == null)
            return;
        try {
            bw.write(line);
            bw.write("\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean started() {
        return p != null;
    }
}