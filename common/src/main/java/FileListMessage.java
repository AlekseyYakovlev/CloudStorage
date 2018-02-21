import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aleksey Yakovlev on 18.02.2018
 * @project CloudStorage
 */
public class FileListMessage extends AbstractMessage {

    @Getter
    private List<File> files;

    public FileListMessage( Path path ) throws IOException {
        files = Files.list(path).map(Path::toFile).collect(Collectors.toList());
    }
}
