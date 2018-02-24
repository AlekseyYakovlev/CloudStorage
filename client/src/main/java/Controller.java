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
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

//TODO: Разделить контроллер отображения и работу с сетью

@Log
public class Controller implements Initializable {
    private static final String REPOSITORY_DIR = "client/local_storage";

    @FXML
    HBox authPanel, actionPanel1, actionPanel2;

    @Getter
    @FXML
    TextField loginField;

    @Getter
    @FXML
    PasswordField passField;

    @Getter
    @FXML
    ListView<File> cloudList, localList;


    @Getter
    @FXML
    ProgressBar progressBar;

    private Socket socket;

    @Getter
    private ObjectOutputStream out;

    private boolean authorized;

    @Getter
    private ObservableList<File> cloudFilesList;
    private ObservableList<File> localFilesList;


    @Override
    public void initialize( URL location, ResourceBundle resources ) {
        setAuthorized(false);
        cloudFilesList = FXCollections.observableArrayList();
        cloudList.setItems(cloudFilesList);
        localFilesList = FXCollections.observableArrayList();
        localList.setItems(localFilesList);
        refreshLocalList();

        localList.setOnDragOver(event -> {
            if (event.getGestureSource() != localList && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        localList.setOnDragDropped(event -> {
            Dragboard drb = event.getDragboard();
            boolean success = false;
            if (drb.hasFiles()) {
                BaseFileOperations.copyDraggedFilesToDir(drb.getFiles(), REPOSITORY_DIR);
                refreshLocalList();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }


    public void setAuthorized( boolean authorized ) {
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
            Thread th = new Thread(() -> {
                try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                    while (true) {
                        log.fine("Preparing to listen");
                        Object obj = in.readObject();
                        log.fine("Have got an object");
                        if (obj instanceof CommandMessage) {
                            CommandMessage cm = (CommandMessage) obj;
                            if (cm.getType() == CommandMessage.CMD_MSG_AUTH_OK) {
                                Controller.this.setAuthorized(true);
                                break;
                            }
                        }
                    }
                    while (true) {
                        if(socket.isClosed()) break;
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
                            //TODO Refactor needed:
                            FilePartitionWorker.receiveFile(in, fm, REPOSITORY_DIR, progressBar);
                            Platform.runLater(Controller.this::refreshLocalList);
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

    public void btnSendFile( ActionEvent actionEvent ) {
        File selectedItem = localList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) new Thread(() -> {
            synchronized (this) {
                FilePartitionWorker.sendFile(out, selectedItem.getAbsolutePath(), progressBar);
            }
        }).start();
    }

    public void tryToAuthorize( ActionEvent actionEvent ) {
        if (socket == null || socket.isClosed()) connect();
        AuthMessage am = new AuthMessage(loginField.getText(), passField.getText());
        sendMsg(am);
    }


    private void sendMsg( AbstractMessage am ) {
        try {
            synchronized (this) {
                out.writeObject(am);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileDownload( ActionEvent actionEvent ) {
        requestCloud(CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD);
    }

    public void btnDeleteCloudFile( ActionEvent actionEvent ) {
        requestCloud(CommandMessage.CMD_MSG_REQUEST_FILE_DELETE);
    }

    public void requestCloud( int commandMessageType ) {
        File file = cloudList.getSelectionModel().getSelectedItem();
        if (file != null) {
            CommandMessage cm = new CommandMessage(commandMessageType, file);
            sendMsg(cm);
        }
    }

    private void refreshCloudList() {
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_REQUEST_FILE_LIST);
        sendMsg(cm);
    }

    public void refreshLocalList() {
        localFilesList.clear();
        localFilesList.addAll(BaseFileOperations.getFileListOfDir(REPOSITORY_DIR));
    }

    public void btnDeleteLocalFile( ActionEvent actionEvent ) {
        File selectedItem = localList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            BaseFileOperations.deleteFile(selectedItem.getAbsolutePath());
            refreshLocalList();
        }
    }

    public void btnRefreshLocal( ActionEvent actionEvent ) {
        refreshLocalList();
    }

    public void btnRefreshCloud( ActionEvent actionEvent ) {
        refreshCloudList();
    }


}
