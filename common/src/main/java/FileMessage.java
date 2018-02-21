import jdk.nashorn.internal.objects.annotations.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * @author Aleksey Yakovlev on 17.02.2018
 * @project CloudStorage
 */
@lombok.Getter
public class FileMessage extends AbstractMessage {
    private String filename;
    private long size;
    private byte[] data;


    public FileMessage( Path path ) throws IOException {
        filename = path.getFileName().toString();
        size = Files.size(path);
        data = Files.readAllBytes(path);
    }

}
