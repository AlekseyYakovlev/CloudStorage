import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private static final String TAG = "client.Controller";
    private static final String REPOSITORY_DIR = "client/local_storage";


    @FXML
    HBox authPanel, actionPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passField;

    @FXML
    ListView<File> mainList;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private boolean authorized;

    private ObservableList<File> filesList;

    @Override
    public void initialize( URL location, ResourceBundle resources ) {
        setAuthorized(false);
        filesList = FXCollections.observableArrayList();
        mainList.setItems(filesList);
        //connect();
    }

    public void setAuthorized( boolean authorized ) {
        this.authorized = authorized;
        authPanel.setManaged(!this.authorized);
        authPanel.setVisible(!this.authorized);
        actionPanel.setManaged(this.authorized);
        actionPanel.setVisible(this.authorized);

    }

    public void connect() {
        try {
            socket = new Socket(ConnectionSettings.SERVER_IP, ConnectionSettings.SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new Thread(() -> {
                try {
                    while (true) {
//                        System.out.println("Preparing to listen");
                        Object obj = in.readObject();
//                        System.out.println("Have got an object");
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
                                filesList.clear();
                                filesList.addAll(flm.getFiles());
                            });
                        }
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            Files.write(Paths.get(REPOSITORY_DIR + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
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
//                        try {
//                            socket.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnSendFile( ActionEvent actionEvent ) {
        try {
            FileMessage fm = new FileMessage(Paths.get("client/1.txt"));
            sendMsg(fm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuthorize( ActionEvent actionEvent ) {
        if (socket == null || socket.isClosed()) connect();
        AuthMessage am = new AuthMessage(loginField.getText(), passField.getText());
        sendMsg(am);
    }


    private void sendMsg( AbstractMessage am ) {
        try {
            out.writeObject(am);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileDownload( ActionEvent actionEvent ) {
        File file = mainList.getSelectionModel().getSelectedItem();
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD, file);

        sendMsg(cm);
    }
}
