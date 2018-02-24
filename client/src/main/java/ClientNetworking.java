import javafx.application.Platform;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Log
public class ClientNetworking {
    private final Controller controller;
    private Socket socket;
    private ObjectOutputStream out;

    ClientNetworking( Controller controller ) {
        this.controller = controller;
    }

    private void connect() {
        try {
            socket = new Socket(ConnectionSettings.SERVER_IP, ConnectionSettings.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            Thread th = new Thread(() -> {
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    while (true) {
                        log.fine("Preparing to listen");
                        Object obj = in.readObject();
                        log.fine("Have got an object");
                        if (obj instanceof CommandMessage) {
                            CommandMessage cm = (CommandMessage) obj;
                            if (cm.getType() == CommandMessage.CMD_MSG_AUTH_OK) {
                                controller.setAuthorized(true);
                                break;
                            }
                        }
                    }
                    while (true) {
                        if (socket.isClosed()) break;
                        log.fine("Preparing to listen");
                        Object obj = in.readObject();
                        log.fine("Have got an object");
                        if (obj instanceof FileListMessage) {
                            FileListMessage flm = (FileListMessage) obj;
                            Platform.runLater(() -> controller.updateCloudFilesList(flm.getFiles()));
                        }
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            FilePartitionWorker.receiveFile(in, fm, Controller.getClientLocalStorage(), controller.getProgressBar());
                            Platform.runLater(controller::refreshLocalList);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            th.setDaemon(true);
            th.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestAuthorization(String login, String pass){
        if (socket == null || socket.isClosed()) connect();
        AuthMessage am = new AuthMessage(login, pass);
        sendMsg(am);
    }


    private void sendMsg( AbstractMessage am ) {
        try {
            synchronized (controller) {
                out.writeObject(am);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileDownload( File file ) {
        requestCloud(CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD,file);
    }

    public void requestDeleteCloud( File file ) {
        requestCloud(CommandMessage.CMD_MSG_REQUEST_FILE_DELETE,file);
    }

    private void requestCloud( int commandMessageType, File file ) {
        if (file != null) {
            CommandMessage cm = new CommandMessage(commandMessageType, file);
            sendMsg(cm);
        }
    }

    void refreshCloudList() {
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_REQUEST_FILE_LIST);
        sendMsg(cm);
    }

    public void SendFile( File selectedItem ) {
        if (selectedItem != null) new Thread(() -> {
            synchronized (this) {
                FilePartitionWorker.sendFile(out, selectedItem.getAbsolutePath(), controller.getProgressBar());
            }
        }).start();
    }


}