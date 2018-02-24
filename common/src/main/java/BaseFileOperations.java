import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Aleksey Yakovlev on 24.02.2018
 * @project CloudStorage
 */
@Log
public class BaseFileOperations {

    public static String createDirIfNone( String... paths ) {
        StringBuilder path = new StringBuilder(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            path.append(File.separator).append(paths[i]);
        }

        if (Files.notExists(Paths.get(path.toString()))) {
            try {
                Files.createDirectory(Paths.get(path.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path.toString();
    }

    public static void deleteFile( String path ) {
        try {
            Files.delete(Paths.get(path));
            log.fine("Deleted file: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile( CommandMessage cm ) {
        deleteFile(((File) cm.getAttachment()[0]).getAbsolutePath());
    }

    public static void deleteFile( File file ) {
        if (file != null) deleteFile(file.getAbsolutePath());
    }

    public static List<File> getFileListOfDir( String dir ) {
        try {
            return Files.list(Paths.get(dir)).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void copyDraggedFilesToDir( List<File> draggedFileList, String dir ) {
        for (File draggedFile : draggedFileList) {
            try {
                Path sourcePath = Paths.get(draggedFile.getAbsolutePath());
                Path destPath = Paths.get(dir + "/" + draggedFile.getName());
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
