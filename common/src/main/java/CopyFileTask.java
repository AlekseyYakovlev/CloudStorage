import javafx.concurrent.Task;
import lombok.Setter;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Setter
public class CopyFileTask extends Task <Boolean>{
    private static final int PART_SIZE = ConnectionSettings.PACKAGE_SIZE_FOR_FILE_TRANSFER;
    private Path path;
    private ObjectOutputStream out;

    @Override
    protected Boolean call() throws Exception {
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
            this.updateProgress(i,partsCount);
        }
        return true;
    }
}
