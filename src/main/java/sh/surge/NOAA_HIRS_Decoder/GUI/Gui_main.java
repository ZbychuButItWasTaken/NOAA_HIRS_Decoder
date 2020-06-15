package sh.surge.NOAA_HIRS_Decoder.GUI;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sh.surge.NOAA_HIRS_Decoder.Main;

import java.io.File;
import java.io.IOException;

public class Gui_main extends Application {
    private static Stage stage;
    private static File file;
    @FXML
    private TextField path;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.out.println(this.getClass());
        AnchorPane root = null;
        root = FXMLLoader.load(this.getClass().getResource("/main.fxml"));
        root.requestFocus();
        primaryStage.setTitle("NOAA HIRS Decoder");
        primaryStage.setScene(new Scene(root, 600, 385));
        primaryStage.show();
        stage = primaryStage;
    }

    public void select_file(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        file = fileChooser.showOpenDialog(stage);
        path.setText(file.getPath());
    }

    public void start_decoding(ActionEvent actionEvent) {
        Main.main(new String[]{path.getText()});
    }
}
