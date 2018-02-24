import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Aleksey Yakovlev on 17.02.2018
 * @project CloudStorage
 */
class Server {
    private static final String TAG = "server.Server";
    private ServerSocket serverSocket;

    public Server() {
        System.out.println("Server started");

        try {
            serverSocket = new ServerSocket(ConnectionSettings.SERVER_PORT);
            System.out.println("Waiting for connection");
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Client is connected");
                new ClientHandler(this,socket);
            }


        } catch (IOException e) {
            System.out.println("Server exception");
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }
}
