import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.fxml.FXML;

import dihedralutils.*;

public class Controller {
  final Group world = new Group();
  final Group axes = new Group();
  final Group sequence = new Group();
  final PerspectiveCamera cam = new PerspectiveCamera(true);

  // camera related fields
  final int VIEWPORT_SIZE = 800;
  final double CAM_INIT_DISTANCE = 100;
  final double CAM_NEAR_CLIP = 0.1;
  final double CAM_FAR_CLIP = 1000;
  final double CAM_MIN_ZOOM = 30;
  final double CAM_MAX_ZOOM = 500;
  final double AXIS_LENGTH = 20;
  // note that camera move speed will vary with zoom level
  double cam_speed = Math.abs(CAM_INIT_DISTANCE / 1000.0);

  // color related fields
  final PhongMaterial red = new PhongMaterial(Color.web("ec5f66"));
  final PhongMaterial orange = new PhongMaterial(Color.web("f9ae58"));
  final PhongMaterial yellow = new PhongMaterial(Color.web("f9d547"));
  final PhongMaterial green = new PhongMaterial(Color.web("99c794"));
  final PhongMaterial blue = new PhongMaterial(Color.web("6699cc"));
  final PhongMaterial purple = new PhongMaterial(Color.web("c695c6"));
  final PhongMaterial grey = new PhongMaterial(Color.web("d8dee9"));
  final PhongMaterial white = new PhongMaterial(Color.WHITE);


  // the 2D UI
  @FXML private BorderPane app;

  @FXML
  private SubScene initView(Group group) {
    SubScene view = new SubScene(
      group,
      1024,
      768,
      true,
      null
    );
    cam.setNearClip(CAM_NEAR_CLIP);
    cam.setFarClip(CAM_FAR_CLIP);
    cam.setTranslateZ(CAM_INIT_DISTANCE);
    // makes camera movement behave as expected with positive x to the right,
    // positive y upward, and positive z coming out of the screen
    cam.setRotationAxis(Rotate.X_AXIS);
    cam.setRotate(180);
    view.setCamera(cam);
    view.setFill(Color.web("111111"));
    return view;
  }

  @FXML
  public void sizeView(SubScene scene) {
    Pane parent = (Pane) scene.getParent();
    scene.widthProperty().bind(parent.widthProperty());
    scene.heightProperty().bind(parent.heightProperty());
  }

  @FXML
  public void buildAxes() {
    final Box xAxis = new Box(AXIS_LENGTH, 0.1, 0.1);
    final Box yAxis = new Box(0.1, AXIS_LENGTH, 0.1);
    final Box zAxis = new Box(0.1, 0.1, AXIS_LENGTH);
    xAxis.setMaterial(red);
    yAxis.setMaterial(green);
    zAxis.setMaterial(blue);
    axes.getChildren().addAll(xAxis, yAxis, zAxis);
    axes.setVisible(false);
    world.getChildren().addAll(axes);
  }

  @FXML
  public Link[] buildLinks(int n) {
    Link[] links = new Link[n];
    Angular[] pos = new Angular[n];
    for (int i = 0; i < n; i++) {
      Angular p = new Angular();
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

    Cartesian[] pdb = DihedralUtility.pos2pdb(pos);
    boolean isEndNode = false;
    for (int i = 0; i < n; i++) {
      Cartesian p = pdb[i];
      if (i == n-1) isEndNode = true;
      Link l = new Link(p.ca.x, p.ca.y, p.ca.z, isEndNode);
      links[i] = l;
    }
    for (int i = 0; i < n-1; i++) {
      links[i].rotateRod(links[i+1]);
    }
    return links;
  }

  @FXML
  public void buildSequence() {
    Link[] links = buildLinks(20);
    for (Link l : links) {
      l.node.setMaterial(purple);
      sequence.getChildren().add(l);
    }
    // REMOVE LATER
    // sequence.setRotationAxis(Rotate.Y_AXIS);
    world.getChildren().addAll(sequence);
  }

  // UI
  @FXML private Slider thetaSlider;
  @FXML private Slider taoSlider;
  @FXML private ImageView piPNG1;
  @FXML private ImageView piPNG2;
  @FXML private ImageView negPiPNG;
  @FXML private ImageView zeroPNG;

  private final double PI = Math.PI;

  @FXML
  public void initSliders() {
    thetaSlider.setMin(0);
    thetaSlider.setMax(PI);
    thetaSlider.setValue(PI/2.0);

    taoSlider.setMin(-PI);
    taoSlider.setMax(PI);
    taoSlider.setValue(0);
  }

  @FXML
  public void initPNGs() {
    zeroPNG.setFitWidth(50);
    zeroPNG.setFitHeight(50);
    piPNG1.setFitWidth(50);
    piPNG1.setFitHeight(50);
    negPiPNG.setFitWidth(50);
    negPiPNG.setFitHeight(50);
    piPNG2.setFitWidth(50);
    piPNG2.setFitHeight(50);
  }

  // mouse movement fields
  double mouseXf;
  double mouseYf;
  double mouseX0;
  double mouseY0;
  double mouseDeltaX;
  double mouseDeltaY;

  // node selection fields
  Sphere selectedNode;
  PhongMaterial selectedMaterial;

  @FXML
  private void handle3DMouse(SubScene scene, final Node root) {
    scene.setOnMousePressed(new EventHandler<MouseEvent> () {
      @Override
      public void handle(MouseEvent e) {
        // update mouse position
        mouseXf = e.getSceneX();
        mouseYf = e.getSceneY();
        // update selection
        PickResult result = e.getPickResult();
        if (result.getIntersectedNode() instanceof Sphere) {
          // reset color of previous node
          if (selectedNode != null) {
            selectedNode.setMaterial(selectedMaterial);
          }
          // set color of selection if not null
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

    scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent e) {
        // new x,y naught is previous x,y final
        mouseX0 = mouseXf;
        mouseY0 = mouseYf;
        mouseXf = e.getSceneX();
        mouseYf = e.getSceneY();
        mouseDeltaX = mouseXf - mouseX0;
        mouseDeltaY = mouseYf - mouseY0;

        if (e.isPrimaryButtonDown()) {
          cam.setTranslateX(cam.getTranslateX() - mouseDeltaX*cam_speed);
          cam.setTranslateY(cam.getTranslateY() + mouseDeltaY*cam_speed);
        }
        // REMOVE LATER
        // if (e.isSecondaryButtonDown()) {
        //   sequence.setRotate(sequence.getRotate() + mouseDeltaX);
        // }
      }
    });

    scene.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle(ScrollEvent e) {
        double zoom = cam.getTranslateZ() - e.getDeltaY();

        if (!e.isInertia()) {
          if (zoom <= CAM_MIN_ZOOM) {
            cam.setTranslateZ(CAM_MIN_ZOOM);
            cam_speed = Math.abs(cam.getTranslateZ() / 1000.0);
          }
          else if (zoom >= CAM_MAX_ZOOM) {
            cam.setTranslateZ(CAM_MAX_ZOOM);
            cam_speed = Math.abs(cam.getTranslateZ() / 1000.0);
          }
          else {
            cam.setTranslateZ(cam.getTranslateZ() - e.getDeltaY());
            cam_speed = Math.abs(cam.getTranslateZ() / 1000.0);
          }
        }
      }
    });
  }

  @FXML
  public void initialize() {
    buildAxes();
    buildSequence();
    initSliders();
    initPNGs();
    SubScene view = initView(world);
    Pane viewport = new Pane(view);
    sizeView(view);
    app.setCenter(viewport);
    handle3DMouse(view, world);
  }
}
