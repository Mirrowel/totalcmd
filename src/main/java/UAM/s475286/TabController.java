package UAM.s475286;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabController implements Initializable {
    @FXML
    TableView<FileObject> filesTable;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileObject, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileObject, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileObject, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileObject, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileObject, String> fileDateColumn = new TableColumn<>("Date");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);


        ObjectProperty<TableRow<FileObject>> lastSelectedRow = new SimpleObjectProperty<>();

        filesTable.setRowFactory(tableView -> {
            TableRow<FileObject> row = new TableRow<FileObject>();

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    lastSelectedRow.set(row);
                }
            });
            return row;
        });


        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Bounds boundsOfSelectedRow = lastSelectedRow.get().localToScene(lastSelectedRow.get().getLayoutBounds());
                if (boundsOfSelectedRow.contains(event.getSceneX(), event.getSceneY()) == false) {
                    //filesTable.getSelectionModel().clearSelection();
                    return;
                } //- to clear selection when selecting empty space
                FileObject selected = filesTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    System.out.println("click on " + filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (event.getClickCount() >= 2) {
                        mouseClick();
                    }
                }
            }
        });

        filesTable.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                Bounds boundsOfSelectedRow = lastSelectedRow.get().localToScene(lastSelectedRow.get().getLayoutBounds());
                if (boundsOfSelectedRow.contains(event.getSceneX(), event.getSceneY()) == false) {
                    //filesTable.getSelectionModel().clearSelection();
                    return;
                } //- to clear selection when selecting empty space
                Path path = Paths.get(getCurrentPath(), getSelectedFilename());
                if (path != null) {
                    System.out.println("MOUSE DRAG on " + path);
                    Dragboard db = filesTable.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(path.toString());
                    db.setContent(content);
                    event.consume();
                }
            }
        });

        filesTable.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                //System.out.println("MOUSE DRAG OVER on " + db.getString());
                if (event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        filesTable.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                System.out.println("MOUSE DRAG DROP on " + db.getString());
                boolean success = false;
                if (db.hasString()) {
                    Path src = Paths.get(db.getString());

                    File f = new File(db.getString());
                    Path dest = Paths.get(getCurrentPath() + "/" + f.getName());
                    try (Stream<Path> stream = Files.walk(src)) {
                        stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
                        success = true;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                event.setDropCompleted(success);
                event.consume();
                updateList(Paths.get(getCurrentPath()));
            }
        });

        filesTable.sortPolicyProperty().set(t -> {
            Comparator<FileObject> comparator = (r1, r2) ->
                    r1.getFilename().equals("...") ? -1
                            : r2.getFilename().equals("...") ? 1
                            : t.getComparator() == null ? 0
                            : t.getComparator().compare(r1, r2);

            if (t.getItems() == null)
                return false;
            FXCollections.sort(t.getItems(), comparator);
            return true;
        });


        updateList(Paths.get("."));
    }

    public void mouseClick() {
        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
        if (filesTable.getSelectionModel().getSelectedItem().getFilename() == "...") {
            //System.out.println("...");
            btnPathUpAction();
        } else if (Files.isDirectory(path) && filesTable.getSelectionModel().getSelectedItem().getFilename() != "...") {
            //System.out.println("updated");
            updateList(path);
        }
    }

    public void updateList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();

            Path upperPath = Paths.get(pathField.getText()).getParent();
            if (upperPath != null) {
                filesTable.getItems().add(new FileObject(upperPath));
            }
            filesTable.getItems().addAll(Files.list(path).map(FileObject::new).collect(Collectors.toList()));
            if (upperPath != null) {
                filesTable.getItems().get(0).setFilename("...");
            }
            filesTable.sort();
            filesTable.getSelectionModel().selectFirst();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Couldn't refresh file list", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction() {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }

    public TableView<FileObject> getFileInfo() {
        return filesTable;
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
