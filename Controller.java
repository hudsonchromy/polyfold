import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;
import javafx.fxml.FXML;
import java.util.*;
import java.io.*;

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
  final double CAM_FAR_CLIP = 2000;
  final double CAM_MIN_ZOOM = 30;
  final double CAM_MAX_ZOOM = 1000;
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

  // node related fields;
  private Link[] links;
  private Angular[] angles;
  private Cartesian[] carts;

  // the 2D UI
  @FXML private BorderPane app;

  // initializes subscene for 3D graphics
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
    view.setFill(Color.web("1c2025"));
    return view;
  }

  // keeps the 3D subscene dimensions the same as its parent pane
  public void sizeView(SubScene scene) {
    Pane parent = (Pane) scene.getParent();
    scene.widthProperty().bind(parent.widthProperty());
    scene.heightProperty().bind(parent.heightProperty());
  }

  // builds 3D axes for aiding orientation
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

  public void setCameraZoom() {
    // cam field of view is 30 degrees - pi / 6
    double tanTheta = Math.tan(Math.PI / 6.0);
    // keep track of max zoom level needed to see all nodes
    double maxZoom, leftMost, rightMost, upMost, downMost;
    maxZoom = -1;
    leftMost = 1000000;
    rightMost = -1000000;
    upMost = -1000000;
    downMost = 1000000;

    for (Link l : links) {
      // get position of sphere
      double x, y, xZoom, yZoom;
      x = l.getTranslateX();
      y = l.getTranslateY();
      leftMost = Math.min(leftMost, x);
      rightMost = Math.max(rightMost, x);
      upMost = Math.max(upMost, y);
      downMost = Math.min(downMost, y);
      xZoom = Math.abs(x / tanTheta) + 100;
      yZoom = Math.abs(y / tanTheta) + 100;
      maxZoom = Math.max(maxZoom, xZoom);
      maxZoom = Math.max(maxZoom, yZoom);
    }
    cam.setTranslateX((leftMost + rightMost) / 2.0);
    cam.setTranslateY((upMost + downMost) / 2.0);
    cam.setTranslateZ(maxZoom);
  }

  // maps a secondary structure character to its theta angles
  HashMap<Character, Integer> secondaryTheta = new HashMap<Character, Integer>();
  // maps a secondary structure character to its tao angle
  HashMap<Character, Integer> secondaryTao = new HashMap<Character, Integer>();
  // initialize maps for secondary structure to angle
  public void initSecondary() {
    secondaryTheta.put('H', 89);
    secondaryTheta.put('E', 124);
    secondaryTheta.put('C', 110);
    secondaryTao.put('H', 50);
    secondaryTao.put('E', -170);
    secondaryTao.put('C', -150);
  }

  // helper function for building the sequence
  public Link[] buildLinks(String content, String extension) {
    int n = (content != null) ? content.length() : 0;
    links = new Link[n];
    angles = new Angular[n];
    // handle each character in file
    for (int i = 0; i < n; i++) {
      Angular a = new Angular();
      a.id = i;
      // first and last node have no theta
      if (i == 0 || i == n-1) {
        a.theta = 2 * Math.PI;
      }
      else if (content != null && extension.equals(".ss")) {
        a.theta = secondaryTheta.get(content.charAt(i)) * Math.PI / 180.0;
      }
      // no string provided
      else {
        a.theta = 110.0 * Math.PI / 180.0;
      }
      // first, second to last, and last nodes have no tao
      if (i == 0 || i == n-1 || i == n-2) {
        a.tao = 2 * Math.PI;
      }
      else if (content != null && extension.equals(".ss")) {
        a.tao = secondaryTao.get(content.charAt(i)) * Math.PI / 180.0;
      }
      // no string provided
      else {
        a.tao = -150.0 * Math.PI / 180.0;
      }
      angles[i] = a;
    }

    // convert to cartesion points
    carts = DihedralUtility.angles2Carts(angles);
    boolean isEndNode = false;
    for (int i = 0; i < n; i++) {
      Cartesian c = carts[i];
      if (i == n-1) isEndNode = true;
      Link l = new Link(c.pos.x, c.pos.y, c.pos.z, isEndNode);
      l.id = i;
      links[i] = l;
    }
    // connect rods to spheres
    for (int i = 0; i < n-1; i++) {
      links[i].rotateRod(links[i+1]);
    }
    return links;
  }

  // add all the links
  public void buildSequence(File f) throws IOException {
    sequence.getChildren().clear();
    world.getChildren().clear();
    String extension = getExtension(f);
    String content = new Scanner(f).useDelimiter("\\A").next();
    // build links
    Link[] links = buildLinks(content, extension);
    for (Link l : links) {
      l.node.setMaterial(purple);
      sequence.getChildren().add(l);
    }
    sequence.setRotationAxis(Rotate.Y_AXIS);
    world.getChildren().addAll(sequence);
    setCameraZoom();
  }

  // helper function to get file extension
  public String getExtension(File f) {
    String name = f.getName();
    int index = name.lastIndexOf(".");
    if (index == -1) return null;
    return name.substring(index);
  }

  // UI
  @FXML private Slider thetaSlider;
  @FXML private Slider taoSlider;
  @FXML private ImageView piPNG1;
  @FXML private ImageView piPNG2;
  @FXML private ImageView negPiPNG;
  @FXML private ImageView zeroPNG;

  private final double PI = Math.PI;

  // disables sliders and puts the selector halfway
  public void resetSliders() {
    thetaSlider.setMin(0);
    thetaSlider.setMax(PI);
    thetaSlider.setValue(PI/2.0);
    thetaSlider.setDisable(true);

    taoSlider.setMin(-PI);
    taoSlider.setMax(PI);
    taoSlider.setValue(0);
    taoSlider.setDisable(true);
  }

  // updates a slider with the angle info of a give node
  public void updateSliders(double theta, double tao, int id) {
    if (id == 0 || id == angles.length-1) {
      resetSliders();
      return;
    }
    if (id == angles.length-2) {
      thetaSlider.setDisable(false);
      thetaSlider.setValue(theta);
      return;
    }
    thetaSlider.setDisable(false);
    taoSlider.setDisable(false);
    thetaSlider.setValue(theta);
    taoSlider.setValue(tao);
  }

  // gets png file assets and sets their size
  public void initPNGs() {
    // zero png looks better slightly smaller
    zeroPNG.setFitWidth(35);
    zeroPNG.setFitHeight(35);
    piPNG1.setFitWidth(40);
    piPNG1.setFitHeight(40);
    negPiPNG.setFitWidth(40);
    negPiPNG.setFitHeight(40);
    piPNG2.setFitWidth(40);
    piPNG2.setFitHeight(40);
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
          // set color of selection
          selectedNode = (Sphere) result.getIntersectedNode();
          selectedMaterial = (PhongMaterial) selectedNode.getMaterial();
          selectedNode.setMaterial(green);
          Link link = (Link) selectedNode.getParent();
          int id = link.id;
          updateSliders(angles[id].theta, angles[id].tao, id);
          return;
        }
        if (selectedNode != null) {
          selectedNode.setMaterial(selectedMaterial);
        }
        selectedNode = null;
        selectedMaterial = null;
        resetSliders();
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
        // left click moves the camera
        if (e.isPrimaryButtonDown()) {
          cam.setTranslateX(cam.getTranslateX() - mouseDeltaX*cam_speed);
          cam.setTranslateY(cam.getTranslateY() + mouseDeltaY*cam_speed);
        }
        // right click rotates entire sequence around y axis
        if (e.isSecondaryButtonDown()) {
          sequence.setRotate(sequence.getRotate() + mouseDeltaX);
        }
      }
    });

    scene.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle(ScrollEvent e) {
        double zoom = cam.getTranslateZ() - e.getDeltaY();
        // scrolling zooms the camera
        if (!e.isInertia()) {
          if (zoom <= CAM_MIN_ZOOM) {
            cam.setTranslateZ(CAM_MIN_ZOOM);
            cam_speed = Math.abs(cam.getTranslateZ() / 1000.0);
          }
          // check zoom bounds
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

  // open an amino acid or secondary structure file
  @FXML
  private void openFile(ActionEvent e) throws IOException {
    FileChooser fileModal = new FileChooser();
    fileModal.setTitle("Open Resource File");
    fileModal.getExtensionFilters().addAll(
      new ExtensionFilter("Amino Acid (.aa)", "*.aa"),
      new ExtensionFilter("Secondary Structure (.ss)", "*.ss"),
      new ExtensionFilter("Contact Map (.rr)", "*.rr"),
      new ExtensionFilter("Protein Data Bank (.pdb)", "*.pdb")
    );
    File f = fileModal.showOpenDialog(app.getScene().getWindow());
    if (f != null) {
      buildSequence(f);
    }
  }

  public void initialize() throws IOException {
    // NOT NEEDED - FOR INTERNAL USE
    // buildAxes();
    initPNGs();
    initSecondary();
    resetSliders();
    SubScene view = initView(world);
    Pane viewport = new Pane(view);
    sizeView(view);
    app.setCenter(viewport);
    handle3DMouse(view, world);
  }
}
