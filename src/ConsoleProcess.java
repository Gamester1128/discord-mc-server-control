import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ConsoleProcess {

    private ProcessBuilder pb;
    private Process p;
    private BufferedWriter bw;
    private BufferedReader br;

    public ConsoleProcess(String commandToExecute) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.out.println(System.getProperty("os.name"));
            pb = new ProcessBuilder("cmd.exe", "/c", commandToExecute);
        } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            pb = new ProcessBuilder("/bin/bash", "-c", commandToExecute);
        }
        else {
            System.out.println(
                    "What niche OS you using there bud.. or how ignorant am I? OS: " + System.getProperty("os.name"));
            System.exit(1);
        }
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);
    }

    public void start() {
        try {
            System.out.println(pb.directory());
            p = pb.start();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void end() {
        try {
            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        p.destroy();
    }

    public String readLine() {
        try {
            String line = br.readLine();
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeLine(String line) {
        try {
            bw.write(line);
            bw.write("\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}