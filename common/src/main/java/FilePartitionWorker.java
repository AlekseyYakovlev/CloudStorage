import lombok.extern.java.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * @author Aleksey Yakovlev on 21.02.2018
 * @project CloudStorage
 */
@Log
public class FilePartitionWorker {
    private static final int PART_SIZE = ConnectionSettings.PACKAGE_SIZE_FOR_FILE_TRANSFER;

    public static void sendFile( Path path, ObjectOutputStream out ) {
        try {
            byte[] fileData = Files.readAllBytes(path);
            int partsCount = fileData.length / PART_SIZE;
            if (fileData.length % PART_SIZE != 0) partsCount++;

            for (int i = 0; i < partsCount; i++) {
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
        } catch (IOException e) {
            log.severe("Error: IOException while sending file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void receiveFile( ObjectInputStream in, FileMessage fm, Path path ) {
        try {
            if (Files.exists(path)) Files.delete(path);
            Files.createFile(path);

            for (int i = 0; i < fm.getPartsCount(); i++) {
                if (i > 0) fm = (FileMessage) in.readObject();
                Files.write(path, fm.getData(), StandardOpenOption.APPEND);
            }
            log.fine("File: "+path.toString() + " received in "+fm.getPartsCount()+" parts");
        } catch (Exception e) {
            log.severe("Error: IOException while receiving file: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
