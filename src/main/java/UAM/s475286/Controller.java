package UAM.s475286;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;


public class Controller {
    @FXML
    private VBox leftPanel, rightPanel;

    @FXML

    public void btnExitAction() {
        Platform.exit();
    }

    public void copyBtnAction() throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No files selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
        tryCopy(srcPath, dstPath);
        dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
    }

    private void tryCopy(Path srcPath, Path dstPath) throws IOException {
        System.out.println(srcPath);
        System.out.println(dstPath);
        System.out.println();
        try {
            if (srcPath.toFile().isDirectory()) {
                copyFolder(srcPath, dstPath);
            } else {
                Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException ie) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot copy current file", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void copyFolder(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void moveBtnAction() throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No files selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
        tryCopy(srcPath, dstPath);
        tryDelete(srcPath);
        UpdateLists();
    }

    public void deleteBtnAction(){
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No files selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        tryDelete(srcPath);
        srcPC.updateList(Paths.get(srcPC.getCurrentPath()));
    }

    private void tryDelete(Path srcPath) {
        try {
            DeleteFolder(srcPath);
            Files.deleteIfExists(srcPath);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot delete selected file", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void DeleteFolder(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @FXML
    private void createBtnAction() {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());

        try {
            TextInputDialog dialog = new TextInputDialog("New folder");
            dialog.setTitle("Create new folder");
            dialog.setContentText("New folder (directory)");

            Optional<String> result = dialog.showAndWait();

            if (!result.isPresent())
                return;
            if (result.get().isEmpty())
                throw new IOException("Name is empty");
            if(!(new File(srcPath.toString()).canWrite()))
                throw new IOException("Writing not allowed");

            Files.createDirectories(Paths.get(srcPC.getCurrentPath() + "/" + result.get()));
            UpdateLists();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void UpdateLists() {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");
        leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
        rightPC.updateList(Paths.get(rightPC.getCurrentPath()));
    }

    public void mouseClickCtrl(){
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No files selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        srcPC.mouseClick();
    }

    @FXML
    private void handleOnKeyPressed(KeyEvent event) throws IOException {
        String type = event.getEventType().getName();
        KeyCode keyCode = event.getCode();

        //System.out.println("Type: " + type + " Code: " + keyCode);
        if (keyCode == KeyCode.F5 || keyCode == KeyCode.F6 || keyCode == KeyCode.F7 || keyCode == KeyCode.F8 || keyCode == KeyCode.ENTER || (keyCode == KeyCode.R && event.isControlDown())) {
            System.out.println("Type: " + type + " Code: " + keyCode);
            if (keyCode == KeyCode.F5) {
                copyBtnAction();
            } else if (keyCode == KeyCode.F6) {
                moveBtnAction();
            } else if (keyCode == KeyCode.F7) {
                createBtnAction();
            } else if (keyCode == KeyCode.F8) {
                deleteBtnAction();
            } else if (keyCode == KeyCode.ENTER) {
                mouseClickCtrl();
            } else if (keyCode == KeyCode.R) {
                UpdateLists();
            }
        }
    }
}
