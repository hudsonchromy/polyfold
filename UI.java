import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;
import java.io.IOException;

public class UI extends Application {

  @Override
  public void start(Stage stage) throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource("ui.fxml"));

    Scene scene = new Scene(root, 1024, 768, true);

    stage.setTitle("UI Frame");
    stage.setScene(scene);
    stage.setResizable(false);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
