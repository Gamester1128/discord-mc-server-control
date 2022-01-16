public class Main {
    
    public static Server server;

    public static void main(String[] args) {
        server = new Server(7272);
        server.run();
    }

}
