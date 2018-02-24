import lombok.Getter;

import java.io.File;
import java.util.List;

/**
 * @author Aleksey Yakovlev on 18.02.2018
 * @project CloudStorage
 */
public class FileListMessage extends AbstractMessage {

    @Getter
    private List<File> files;

    public FileListMessage( List<File> files ){
        this.files = files;
    }
}
