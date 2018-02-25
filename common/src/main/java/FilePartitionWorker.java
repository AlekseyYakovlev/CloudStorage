
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

    private static final int PART_SIZE = ConnectionSettings.PACKAGE_SIZE_FOR_FILE_TRANSFER;


    public static void sendFile( ObjectOutputStream out, String pathString, ProgressBarController progressBar ) {

        try {
            Path path = Paths.get(pathString);
            boolean showProgressBar = progressBar != null;
            if(showProgressBar) progressBar.show(true);

            byte[] fileData = Files.readAllBytes(path);
            int partsCount = fileData.length / PART_SIZE;
            if (fileData.length % PART_SIZE != 0) partsCount++;

            for (int i = 0; i < partsCount; i++) {
                if(showProgressBar) progressBar.update( i, partsCount);
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
            if(showProgressBar) progressBar.show(false);
            log.fine("File: " + path.toString() + " sent in " + partsCount + " parts");
        } catch (IOException e) {
            if(progressBar != null) progressBar.show(false);
            log.severe("Error: IOException while sending file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void receiveFile( ObjectInputStream in, FileMessage fm, String directory, ProgressBarController progressBar ) {
        try {
            Path path = Paths.get(directory + File.separator + fm.getFilename());
            boolean showProgressBar = progressBar != null;
            if(showProgressBar) progressBar.show(true);


            if (Files.exists(path)) Files.delete(path);
            Files.createFile(path);

            int partsCount = fm.getPartsCount();
            for (int i = 0; i < partsCount; i++) {
                if(showProgressBar) progressBar.update( i, partsCount);
                if (i > 0) fm = (FileMessage) in.readObject();
                Files.write(path, fm.getData(), StandardOpenOption.APPEND);

            }
            if(showProgressBar) progressBar.show(false);
            log.fine("File: " + path.toString() + " received in " + partsCount + " parts");
        } catch (Exception e) {
            if(progressBar != null) progressBar.show(false);
            log.severe("Error: IOException while receiving file: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
