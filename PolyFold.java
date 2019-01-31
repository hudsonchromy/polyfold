import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import java.io.IOException;

public class PolyFold extends Application {

  // allows global access to stage
  private static Stage globalStage;

  @Override
  public void start(Stage stage) throws IOException {
    // loads xml
    FXMLLoader loader = new FXMLLoader(getClass().getResource("style/polyfold.fxml"));

    // event controller
    Controller c = new Controller();
    loader.setController(c);

    globalStage = stage;

    // build scene
    Parent root = loader.load();
    Scene scene = new Scene(root, 1024, 768, true);

    // apply style
    scene.getStylesheets().add("style/style.css");

    // build stage
    stage.setTitle("PolyFold (Alpha Version)");
    stage.setScene(scene);
    stage.show();

    // set stage size to be minimum 1024 x 768
    stage.setMinWidth(stage.getWidth());
    stage.setMinHeight(stage.getHeight());
  }

  // takes a String and sets the stage title
  public static void setPrimaryStageTitle(String newTitle) {
    globalStage.setTitle(newTitle);
  }

  public static void minimizeWindow() {
    globalStage.setIconified(true);
  }

  public static void makeFullScreen() {
    globalStage.setFullScreen(true);
  }

  public static boolean checkFullScreen() {
    return globalStage.isFullScreen();
  }

  // ignored in javafx apps
  public static void main(String[] args) {
    launch(args);
  }
}
