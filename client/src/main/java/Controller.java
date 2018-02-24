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
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Log
public class Controller implements Initializable {
    @Getter
    private static final String clientLocalStorage = "client/local_storage";
    private final ClientNetworking clientNetworking = new ClientNetworking(this);

    @FXML
    HBox authPanel, actionPanel1, actionPanel2;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passField;

    @FXML
    ListView<File> cloudList, localList;


    @Getter
    @FXML
    ProgressBar progressBar;

    private boolean authorized;


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

        setDragAndDropListeners();
    }

    private void setDragAndDropListeners() {
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
                BaseFileOperations.copyDraggedFilesToDir(drb.getFiles(), clientLocalStorage);
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

    public void btnSendFile( ActionEvent actionEvent ) {
        File selectedItem = localList.getSelectionModel().getSelectedItem();
        clientNetworking.SendFile(selectedItem);
    }

    public void tryToAuthorize( ActionEvent actionEvent ) {
        clientNetworking.requestAuthorization(loginField.getText(),passField.getText());
    }

    public void btnRequestFileDownload( ActionEvent actionEvent ) {
        clientNetworking.requestFileDownload(cloudList.getSelectionModel().getSelectedItem());
    }

    public void btnDeleteCloudFile( ActionEvent actionEvent ) {
        clientNetworking.requestDeleteCloud(cloudList.getSelectionModel().getSelectedItem());
    }

    public void refreshLocalList() {
        localFilesList.clear();
        localFilesList.addAll(BaseFileOperations.getFileListOfDir(clientLocalStorage));
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
        clientNetworking.refreshCloudList();
    }


    public void updateCloudFilesList( List<File> files ) {
        cloudFilesList.clear();
        cloudFilesList.addAll(files);
    }
}
