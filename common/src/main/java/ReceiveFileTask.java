import javafx.concurrent.Task;
import lombok.Setter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Setter
public class ReceiveFileTask extends Task <Boolean>{
    private static final int PART_SIZE = ConnectionSettings.PACKAGE_SIZE_FOR_FILE_TRANSFER;
    private Path path;
    private ObjectInputStream in;
    private FileMessage fm;


    @Override
    protected Boolean call() throws Exception {
        if (Files.exists(path)) Files.delete(path);
        Files.createFile(path);

        for (int i = 0; i < fm.getPartsCount(); i++) {
            if (i > 0) fm = (FileMessage) in.readObject();
            Files.write(path, fm.getData(), StandardOpenOption.APPEND);
        }

        return true;
    }
}
