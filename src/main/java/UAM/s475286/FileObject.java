package UAM.s475286;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class FileObject {
    public enum FileType {
        FILE("F"), DIRECTORY("D");

        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private File descriptor;
    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastModified;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    private SimpleStringProperty created;

    public FileObject(Path path) {
        try {
            //try {
            if (path.equals(path.getRoot()))
                this.filename = "123";
            else
                this.filename = path.getFileName().toString();
            //} catch (InvocationTargetException exception) {
            //    filename="123";
            //}
            SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            BasicFileAttributes atr = Files.readAttributes(path, BasicFileAttributes.class);
            this.created = new SimpleStringProperty(template.format(atr.creationTime().toMillis()));
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        } catch (IOException e) {
            filename = "123";
            throw new RuntimeException("Unable to create file info from path");
        }
    }
}