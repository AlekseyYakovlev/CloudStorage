

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * @author Aleksey Yakovlev on 17.02.2018
 * @project CloudStorage
 */
@Getter
public class FileMessage extends AbstractMessage {
    private String filename;
    private long size;
    private byte[] data;
    private int partsCount;
    private int partsNumber;


    public FileMessage( Path path ) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    public FileMessage( String filename, byte[] data, int partsCount, int partsNumber ) {
        this.filename = filename;
        this.data = data;
        this.partsCount = partsCount;
        this.partsNumber = partsNumber;
    }
}
