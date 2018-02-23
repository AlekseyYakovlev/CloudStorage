import javafx.event.ActionEvent;
import lombok.extern.java.Log;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

//TODO: 0:45:21

/**
 * @author Aleksey Yakovlev on 17.02.2018
 * @project CloudStorage
 */

@Log
public class ClientHandler {
    private static final String REPOSITORY_DIR = "server/repository";
    private Server server;

    private Socket socket;
    private ObjectOutputStream out;

    private String username;

    public ClientHandler( Server server, Socket socket ) {
        this.server = server;
        this.socket = socket;

        new Thread(() -> {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream thr_out = new ObjectOutputStream(socket.getOutputStream());
            ) {
                this.out = thr_out;
                while (true) {
                    log.fine("Preparing to listen");
                    Object obj = in.readObject();
                    log.fine("Have got an object");
                    if (obj instanceof AuthMessage) {
                        AuthMessage am = (AuthMessage) obj;
                        log.fine("Authorization request");
                        //TODO: Update authorization procedure
                        if (am.getLogin().equals("login") && am.getPass().equals("pass")) {
                            log.fine("Authorization OK");
                            this.username = "client";
                            CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_AUTH_OK);
                            sendMsg(cm);
                            sendFileList();
                            log.fine("Have got an object");
                            break;
                        }
                    }
                }

                while (true) {
                    if(socket.isClosed()) break;
                    Object obj = in.readObject();
                    if (obj instanceof AbstractMessage) {
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            FilePartitionWorker.receiveFile(in,fm,Paths.get(REPOSITORY_DIR + "/" + username + "/" + fm.getFilename()),null);
                            sendFileList();
                            continue;
                        }
                    }

                    if (obj instanceof CommandMessage) {
                        CommandMessage cm = (CommandMessage) obj;
                        log.fine("command message received");
                        if (cm.getType() == CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD) {
                            FilePartitionWorker.sendFile(out, Paths.get(((File) cm.getAttachment()[0]).getAbsolutePath()),null);
                            log.fine("requested file: "+ ((File) cm.getAttachment()[0]).getAbsolutePath());
                            continue;
                        }
                        if(cm.getType() == CommandMessage.CMD_REQUEST_FILE_LIST) {
                            sendFileList();
                            continue;
                        }
                        if(cm.getType() == CommandMessage.CMD_MSG_REQUEST_FILE_DELETE){
                            deleteFile(Paths.get(((File) cm.getAttachment()[0]).getAbsolutePath()));
                            log.fine("Deleted file: "+ ((File) cm.getAttachment()[0]).getAbsolutePath());
                            sendFileList();
                            continue;
                        }
                    }

                }
            } catch ( ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                log.fine("Terminated session of user :"+ username);
            }

        }).start();
        log.fine("New thread started for socket: " + socket.toString());
    }

    private void deleteFile(Path path) {
            try {
                Files.delete(path);

            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private void sendMsg( AbstractMessage cm ) {
        try {
            out.writeObject(cm);
            out.flush();
            log.finest("Message sent");
        } catch (IOException e) {
            log.severe("Error: IOException while sending Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getUserRootPath() {
        return REPOSITORY_DIR + "/" + username;
    }

    public void sendFileList() {
        try {
            FileListMessage flm = new FileListMessage(Paths.get(getUserRootPath()));
            sendMsg(flm);
            log.fine("FileList sent");
        } catch (IOException e) {
            log.severe("Error: IOException while sending FileList: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
