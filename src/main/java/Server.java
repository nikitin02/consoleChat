import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private static ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(8000);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
           shutdown();
        }

    }

    public static void broadcast(String message) {
        for (ConnectionHandler ch: connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (Exception e) {
        }
    }

    static class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter name: ");
                name = in.readLine();
                System.out.println(name + " connected");
                broadcast(name + " joined chat");
                out.println("enter your message");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/quit"))
                    {
                        broadcast(name + " left chat");
                        shutdown();
                        connections.remove(this);
                    } else {
                        broadcast(name + ": " + message);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

       public void sendMessage(String message)  {
            out.println(message);
       }

       public void shutdown() {
           try {
               in.close();
               out.close();
            if (!client.isClosed()) {
                client.close();
            }
           } catch (IOException e) {
               throw new RuntimeException(e);
           }
       }

    }


}
