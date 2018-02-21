import lombok.extern.java.Log;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

//TODO: 1:42:10

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
    //private ObjectInputStream in;

    private String username;

    public ClientHandler( Server server, Socket socket ) {
//        try {
        this.server = server;
        this.socket = socket;
//            this.in = new ObjectInputStream(socket.getInputStream());
//            this.out = new ObjectOutputStream(socket.getOutputStream());

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
                    Object obj = in.readObject();
                    if (obj instanceof AbstractMessage) {
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            Files.write(Paths.get(REPOSITORY_DIR + "/" + username + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        }
                    }

                    if (obj instanceof CommandMessage) {
                        CommandMessage cm = (CommandMessage) obj;
                        System.out.println("command message received");
                        if (cm.getType() == CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD) {
                            FilePartitionWorker.sendFile(Paths.get(((File) cm.getAttachment()[0]).getAbsolutePath()), out);
                            System.out.println("requested "+ ((File) cm.getAttachment()[0]).getAbsolutePath());
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
//                } finally {
////                    try {
////                        in.close();
////                    } catch (IOException e) {
////                        log.warning("Error: IOException while closing in: " + e.getMessage());
////                        e.printStackTrace();
////                    }
//                    try {
//                        out.close();
//                    } catch (IOException e) {
//                        log.warning("Error: IOException while closing out: " + e.getMessage());
//                        e.printStackTrace();
//                    }
////                    try {
////                        this.socket.close();
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
            }

        }).start();
        log.fine("New thread started for socket: " + socket.toString());
//        } catch (IOException e) {
//            log.severe("Error: IOException while operating with socket: " + e.getMessage());
//            e.printStackTrace();
//        }
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
