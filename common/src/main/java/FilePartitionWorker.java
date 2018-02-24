import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * @author Aleksey Yakovlev on 21.02.2018
 * @project CloudStorage
 */


@Log
public class FilePartitionWorker {
    enum TransferType {DOWNLOAD, UPLOAD}

    private static final int PART_SIZE = ConnectionSettings.PACKAGE_SIZE_FOR_FILE_TRANSFER;


    public static void sendFile( ObjectOutputStream out, String pathString, ProgressBar progressBar ) {

        try {
            Path path = Paths.get(pathString);
            showProgressBar(progressBar, true);
            rotateProgressBar(progressBar, TransferType.UPLOAD);
            byte[] fileData = Files.readAllBytes(path);
            int partsCount = fileData.length / PART_SIZE;
            if (fileData.length % PART_SIZE != 0) partsCount++;

            for (int i = 0; i < partsCount; i++) {
                updateProgressBar(progressBar, i, partsCount);
                int startPosition = i * PART_SIZE;
                int endPosition = startPosition + PART_SIZE;
                if (endPosition > fileData.length) endPosition = fileData.length;

                FileMessage fm = new FileMessage(
                        path.getFileName().toString(),
                        Arrays.copyOfRange(fileData, startPosition, endPosition),
                        partsCount,
                        i);
                out.writeObject(fm);
                out.flush();
            }
            showProgressBar(progressBar, false);
            log.fine("File: " + path.toString() + " sent in " + partsCount + " parts");
        } catch (IOException e) {
            showProgressBar(progressBar, false);
            log.severe("Error: IOException while sending file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void receiveFile( ObjectInputStream in, FileMessage fm, String directory, ProgressBar progressBar ) {
        try {
            Path path = Paths.get(directory + File.separator + fm.getFilename());
            showProgressBar(progressBar, true);
            rotateProgressBar(progressBar, TransferType.DOWNLOAD);
            if (Files.exists(path)) Files.delete(path);
            Files.createFile(path);

            int partsCount = fm.getPartsCount();
            for (int i = 0; i < partsCount; i++) {
                updateProgressBar(progressBar, i, partsCount);
                if (i > 0) fm = (FileMessage) in.readObject();
                Files.write(path, fm.getData(), StandardOpenOption.APPEND);

            }
            showProgressBar(progressBar, false);
            log.fine("File: " + path.toString() + " received in " + partsCount + " parts");
        } catch (Exception e) {
            showProgressBar(progressBar, false);
            log.severe("Error: IOException while receiving file: " + e.getMessage());
            e.printStackTrace();
        }

    }
    //TODO Вынести методы работы с прогрес баром в Controller
    private static void showProgressBar( ProgressBar progressBar, boolean state ) {
        if (progressBar == null) return;
        Platform.runLater(() -> {
            if (state) progressBar.setProgress(0.0);
            progressBar.setVisible(state);
            progressBar.setManaged(state);
        });
    }

    private static void rotateProgressBar( ProgressBar progressBar, TransferType type ) {
        if (progressBar == null) return;
        Platform.runLater(() -> progressBar.setRotate((type == TransferType.DOWNLOAD) ? 180.0 : 0.0));
    }

    private static void updateProgressBar( ProgressBar progressBar, int i, int partsCount ) {
        if (progressBar == null) return;
        final double progress = (double) i / partsCount;
        Platform.runLater(() -> progressBar.setProgress(progress));
    }
}
