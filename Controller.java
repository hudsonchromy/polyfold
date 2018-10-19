import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.fxml.FXML;

public class Controller {
  final Group world = new Group();
  final Group axes = new Group();
  final Group sequence = new Group();
  final PerspectiveCamera cam = new PerspectiveCamera(true);

  // camera related fields
  final int VIEWPORT_SIZE = 800;
  final double CAM_INIT_DISTANCE = -100;
  final double CAM_NEAR_CLIP = 0.1;
  final double CAM_FAR_CLIP = 1000;
  final double CAM_MIN_ZOOM = -50;
  final double CAM_MAX_ZOOM = -300;
  final double AXIS_LENGTH = 10;
  // note that camera move speed will vary with zoom level
  double cam_speed = CAM_INIT_DISTANCE / -1000.0;


  // color related fields
  // material color fields
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
    cam.setRotate(180);
    view.setCamera(cam);
    view.setFill(Color.BLACK);
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
    final Box xAxis = new Box(AXIS_LENGTH, 0.5, 0.5);
    final Box yAxis = new Box(0.5, AXIS_LENGTH, 0.5);
    final Box zAxis = new Box(0.5, 0.5, AXIS_LENGTH);

    xAxis.setMaterial(red);
    yAxis.setMaterial(green);
    zAxis.setMaterial(blue);

    axes.getChildren().addAll(xAxis, yAxis, zAxis);
    axes.setVisible(false);
    world.getChildren().addAll(axes);
  }

  @FXML
  public Sphere[] buildNodes(int n) {
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

  @FXML
  public void buildSequence() {
    Sphere[] nodes = buildNodes(10);
    for (Sphere s : nodes) {
      s.setMaterial(purple);
      sequence.getChildren().add(s);
    }
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
  }
}
