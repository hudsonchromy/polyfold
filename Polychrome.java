import java.awt.Dimension;
import java.awt.Toolkit;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.stage.Stage;
import javafx.scene.input.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;

public class Polychrome extends Application {
  final Group root = new Group();
  final Group axisGroup = new Group();
  final Group geneSequence = new Group();
  final Group world = new Group();

  final PerspectiveCamera mainCam = new PerspectiveCamera(true);

  // Camera Related Fields
  static final double CAM_INIT_DISTANCE = -100;
  static final double CAM_MOVE_SPEED = 0.1;
  static final double CAM_NEAR_CLIP = 0.1;
  static final double CAM_FAR_CLIP = 1000;
  static final double CAM_MIN_ZOOM = -50;
  static final double CAM_MAX_ZOOM = -300;
  static final double AXIS_LENGTH = 10;

  // Material Color Fields
  final PhongMaterial red = new PhongMaterial(Color.web("ec5f66"));
  final PhongMaterial orange = new PhongMaterial(Color.web("f9ae58"));
  final PhongMaterial yellow = new PhongMaterial(Color.web("f9d547"));
  final PhongMaterial green = new PhongMaterial(Color.web("99c794"));
  final PhongMaterial blue = new PhongMaterial(Color.web("6699cc"));
  final PhongMaterial purple = new PhongMaterial(Color.web("c695c6"));
  final PhongMaterial grey = new PhongMaterial(Color.web("d8dee9"));
  final PhongMaterial white = new PhongMaterial(Color.WHITE);

  private void buildCam() {
    root.getChildren().add(mainCam);

    mainCam.setNearClip(CAM_NEAR_CLIP);
    mainCam.setFarClip(CAM_FAR_CLIP);
    mainCam.setTranslateZ(CAM_INIT_DISTANCE);
    // makes mouse drag behave as expected
    mainCam.setRotate(180);
  }

  private void buildAxes() {
    final Box xAxis = new Box(AXIS_LENGTH, 0.5, 0.5);
    final Box yAxis = new Box(0.5, AXIS_LENGTH, 0.5);
    final Box zAxis = new Box(0.5, 0.5, AXIS_LENGTH);

    xAxis.setMaterial(red);
    yAxis.setMaterial(green);
    zAxis.setMaterial(blue);

    axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
    axisGroup.setVisible(false);
    world.getChildren().addAll(axisGroup);
  }

  // Mouse Movement Fields - X0 is x naught and Xf is x final.
  double mouseXf;
  double mouseYf;
  double mouseX0;
  double mouseY0;
  double mouseDeltaX;
  double mouseDeltaY;

  // Node Selection Fields
  Sphere selectedNode;
  PhongMaterial selectedMaterial;

  private void handleMouse(Scene scene, final Node root) {
    scene.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        PickResult result = event.getPickResult();
        if (result.getIntersectedNode() instanceof Sphere) {
          // reset color of previously selected
          if (selectedNode != null) {
            selectedNode.setMaterial(selectedMaterial);
          }
          // change color of selection
          selectedNode = (Sphere) result.getIntersectedNode();
          selectedMaterial = (PhongMaterial) selectedNode.getMaterial();
          selectedNode.setMaterial(green);
          return;
        }
        if (selectedNode != null) {
          selectedNode.setMaterial(selectedMaterial);
        }
        selectedNode = null;
        selectedMaterial = null;
      }
    });

    scene.setOnMousePressed(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        mouseXf = event.getSceneX();
        mouseYf = event.getSceneY();
      }
    });

    scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        // new x,y naught is previous x,y final
        mouseX0 = mouseXf;
        mouseY0 = mouseYf;
        mouseXf = event.getSceneX();
        mouseYf = event.getSceneY();
        mouseDeltaX = mouseXf - mouseX0;
        mouseDeltaY = mouseYf - mouseY0;

        if (event.isPrimaryButtonDown()) {
          mainCam.setTranslateX(mainCam.getTranslateX() + mouseDeltaX*CAM_MOVE_SPEED);
          mainCam.setTranslateY(mainCam.getTranslateY() + mouseDeltaY*CAM_MOVE_SPEED);
        }
      }
    });

    scene.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle(ScrollEvent event) {
        double zoom = mainCam.getTranslateZ() + event.getDeltaY();

        if (!event.isInertia()) {
          if (zoom >= CAM_MIN_ZOOM) {
            mainCam.setTranslateZ(CAM_MIN_ZOOM);
          }
          else if (zoom <= CAM_MAX_ZOOM) {
            mainCam.setTranslateZ(CAM_MAX_ZOOM);
          }
          else {
            mainCam.setTranslateZ(mainCam.getTranslateZ() + event.getDeltaY());
          }
        }
      }
    });
  }

  private Sphere[] buildNodes(int n) {
    Sphere[] nodes = new Sphere[n];
    PosData[] pos = new PosData[n];
    for (int i = 0; i < n; i++) {
      PosData p = new PosData();
      p.id = i;
      if (i == 0 || i == n-1) {
        p.theta = 2 * Math.PI;
      }
      else {
        p.theta = 110.0 * Math.PI / 180.0;
      }
      if (i == 0 || i == n-1 || i == n-2) {
        p.tao = 2 * Math.PI;
      }
      else {
        p.tao = -150.0 * Math.PI / 180.0;
      }
      pos[i] = p;
    }

    PDBData[] pdb = DihedralUtility.pos2pdb(pos);
    for (int i = 0; i < n; i++) {
      PDBData p = pdb[i];
      Sphere s = new Sphere(1);
      s.setTranslateX(p.ca.x);
      s.setTranslateY(p.ca.y);
      s.setTranslateZ(p.ca.z);
      nodes[i] = s;
    }
    return nodes;
  }

  private void buildSequence() {
    Group sequenceGroup = new Group();
    Sphere[] nodes = buildNodes(10);
    for (Sphere s : nodes) {
      s.setMaterial(purple);
      sequenceGroup.getChildren().add(s);
    }
    geneSequence.getChildren().add(sequenceGroup);
    world.getChildren().addAll(geneSequence);
  }

  @Override
  public void start(Stage primaryStage) {
    root.getChildren().add(world);
    root.setDepthTest(DepthTest.ENABLE);

    buildCam();
    buildAxes();
    buildSequence();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Scene scene = new Scene(root, screenSize.getWidth(), screenSize.getHeight(), true);
    scene.setFill(Color.web("343d46"));
    handleMouse(scene, world);

    primaryStage.setTitle("Polychrome (Alpha Version)");
    primaryStage.setScene(scene);
    primaryStage.show();

    scene.setCamera(mainCam);
  }

  // Ignored in javafx applications
  public static void main(String[] args) {
    launch(args);
  }
}
