import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

/**
 * @author Aleksey Yakovlev on 25.02.2018
 * @project CloudStorage
 */
public class ProgressBarController {
//    private TransferType type;
    private ProgressBar progressBar;

    public void show( boolean state ) {
        Platform.runLater(() -> {
            if (state) progressBar.setProgress(0.0);
            progressBar.setVisible(state);
            progressBar.setManaged(state);
        });
    }

    public void update( int i, int partsCount ) {
        double progress = (double) i / partsCount;
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

//    public enum TransferType {DOWNLOAD, UPLOAD}

    public ProgressBarController(ProgressBar progressBar ) {
//        this.type = type;
        this.progressBar = progressBar;
    }
}
