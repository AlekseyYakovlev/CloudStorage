import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Log
public class Controller implements Initializable {
    private static final String REPOSITORY_DIR = "client/local_storage";

    @FXML
    HBox authPanel, actionPanel1, actionPanel2;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passField;

    @FXML
    ListView<File> cloudList, localList;

    @FXML
    ProgressBar operationProgress;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private boolean authorized;

    private ObservableList<File> cloudFilesList;
    private ObservableList<File> localFilesList;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        cloudFilesList = FXCollections.observableArrayList();
        cloudList.setItems(cloudFilesList);
        localFilesList = FXCollections.observableArrayList();
        localList.setItems(localFilesList);
        refreshLocalList();
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        authPanel.setManaged(!this.authorized);
        authPanel.setVisible(!this.authorized);
        actionPanel1.setManaged(this.authorized);
        actionPanel1.setVisible(this.authorized);
        actionPanel2.setManaged(this.authorized);
        actionPanel2.setVisible(this.authorized);

    }

    public void connect() {
        try {
            socket = new Socket(ConnectionSettings.SERVER_IP, ConnectionSettings.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        log.fine("Preparing to listen");
                        Object obj = in.readObject();
                        log.fine("Have got an object");
                        if (obj instanceof CommandMessage) {
                            CommandMessage cm = (CommandMessage) obj;
                            if (cm.getType() == CommandMessage.CMD_MSG_AUTH_OK) {
                                setAuthorized(true);
                                break;
                            }
                        }
                    }
                    while (true) {
                        Object obj = in.readObject();
                        if (obj instanceof FileListMessage) {
                            FileListMessage flm = (FileListMessage) obj;
                            Platform.runLater(() -> {
                                cloudFilesList.clear();
                                cloudFilesList.addAll(flm.getFiles());
                            });
                        }
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            Path destPath = Paths.get(REPOSITORY_DIR + "/" + fm.getFilename());
                            FilePartitionWorker.receiveFile(in, fm, destPath);
                            Platform.runLater(this::refreshLocalList);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnSendFile(ActionEvent actionEvent) {
        File selectedItem = localList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            FilePartitionWorker.sendFile(Paths.get(selectedItem.getAbsolutePath()), out);
        }
    }

    public void tryToAuthorize(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) connect();
        AuthMessage am = new AuthMessage(loginField.getText(), passField.getText());
        sendMsg(am);
    }


    private void sendMsg(AbstractMessage am) {
        try {
            out.writeObject(am);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileDownload(ActionEvent actionEvent) {
        File file = cloudList.getSelectionModel().getSelectedItem();
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD, file);
        sendMsg(cm);
    }

    public void refreshLocalList() {
        try {
            localFilesList.clear();
            localFilesList.addAll(Files.list(Paths.get(REPOSITORY_DIR)).map(Path::toFile).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnDeleteFile(ActionEvent actionEvent) {
        try {
            File selectedItem = localList.getSelectionModel().getSelectedItem();
            if (selectedItem != null) Files.delete(Paths.get(selectedItem.getAbsolutePath()));
            refreshLocalList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnRefreshLocal(ActionEvent actionEvent) {
        refreshLocalList();
    }

    public void btnRefreshCloud(ActionEvent actionEvent) {
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_REQUEST_FILE_LIST);
        sendMsg(cm);
    }
}
