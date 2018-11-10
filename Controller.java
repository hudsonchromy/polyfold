import javafx.geometry.*;
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
import javafx.scene.text.*;
import javafx.fxml.FXML;
import java.util.*;
import java.io.*;

import dihedralutils.*;

public class Controller {
  // global fields
  final Group world = new Group();
  final Group axes = new Group();
  final Group sequence = new Group();
  final PerspectiveCamera cam = new PerspectiveCamera(true);
  final int INF = 1000000000;
  final double EPS = 0.0000001;

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
  boolean isAutoZoom = false;

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

  @FXML
  public void toggleAutoZoom() {
    isAutoZoom = !isAutoZoom;
    if (isAutoZoom) setCameraZoom();
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
    if (links == null) return;
    // cam field of view is 30 degrees - pi / 6
    double tanTheta = Math.tan(Math.PI / 6.0);
    // keep track of max zoom level needed to see all nodes
    double maxZoom, maxLeft, maxRight, maxUp, maxDown;
    maxZoom = -1;
    // initially set with very high values in opposite direction
    maxLeft = INF;
    maxRight = -INF;
    maxUp = -INF;
    maxDown = INF;

    // finds max left, max right, max up, and max down for calculate camera field of view
    for (Link l : links) {
      // get position of sphere
      double x, y, xZoom, yZoom;
      x = l.getTranslateX();
      y = l.getTranslateY();
      maxLeft = Math.min(maxLeft, x);
      maxRight = Math.max(maxRight, x);
      maxUp = Math.max(maxUp, y);
      maxDown = Math.min(maxDown, y);
      xZoom = Math.abs(x / tanTheta) + 100;
      yZoom = Math.abs(y / tanTheta) + 100;
      maxZoom = Math.max(maxZoom, xZoom);
      maxZoom = Math.max(maxZoom, yZoom);
    }
    cam.setTranslateX((maxLeft + maxRight) / 2.0);
    cam.setTranslateY((maxUp + maxDown) / 2.0);
    cam.setTranslateZ(maxZoom);
  }

  // maps a secondary structure character to its theta angles
  HashMap<Character, Integer> secondaryTheta = new HashMap<Character, Integer>();
  // maps a secondary structure character to its tao angle
  HashMap<Character, Integer> secondaryTao = new HashMap<Character, Integer>();
  // initialize maps for secondary structure to angle
  public void initSecondaryMaps() {
    secondaryTheta.put('H', 89);
    secondaryTheta.put('E', 124);
    secondaryTheta.put('C', 110);
    secondaryTao.put('H', 50);
    secondaryTao.put('E', -170);
    secondaryTao.put('C', -150);
  }

  // maps shorthand characters to full strings for amino acids and secondary structures
  HashMap<Character, String> shorthandAA = new HashMap<Character, String>();
  HashMap<Character, String> shorthandSS = new HashMap<Character, String>();
  HashMap<Character, String> threeCharAA = new HashMap<Character, String>();

  public void initShorthandMaps() {
    shorthandAA.put('A', "Alanine");
    shorthandAA.put('R', "Arginine");
    shorthandAA.put('N', "Asparagine");
    shorthandAA.put('D', "Aspartic Acid");
    shorthandAA.put('B', "Asparagine or Aspartic Acid");
    shorthandAA.put('C', "Cysteine");
    shorthandAA.put('E', "Glutamic Acid");
    shorthandAA.put('Q', "Glutamine");
    shorthandAA.put('Z', "Glutamine or Glutamic Acid");
    shorthandAA.put('G', "Glycine");
    shorthandAA.put('H', "Histidine");
    shorthandAA.put('I', "Isoleucine");
    shorthandAA.put('L', "Leucine");
    shorthandAA.put('K', "Lysine");
    shorthandAA.put('M', "Methionine");
    shorthandAA.put('F', "Phenylalanine");
    shorthandAA.put('P', "Proline");
    shorthandAA.put('S', "Serine");
    shorthandAA.put('T', "Threonine");
    shorthandAA.put('W', "Tryptophan");
    shorthandAA.put('Y', "Tyrosine");
    shorthandAA.put('V', "Valine");

    threeCharAA.put('A', "ALA");
    threeCharAA.put('R', "ARG");
    threeCharAA.put('N', "ASN");
    threeCharAA.put('D', "ASP");
    threeCharAA.put('B', "ASX");
    threeCharAA.put('C', "CYS");
    threeCharAA.put('E', "GLU");
    threeCharAA.put('Q', "GLN");
    threeCharAA.put('Z', "GLX");
    threeCharAA.put('G', "GLY");
    threeCharAA.put('H', "HIS");
    threeCharAA.put('I', "ILE");
    threeCharAA.put('L', "LEU");
    threeCharAA.put('K', "LYS");
    threeCharAA.put('M', "MET");
    threeCharAA.put('F', "PHE");
    threeCharAA.put('P', "PRO");
    threeCharAA.put('S', "SER");
    threeCharAA.put('T', "THR");
    threeCharAA.put('W', "TRP");
    threeCharAA.put('Y', "TYR");
    threeCharAA.put('V', "VAL");

    shorthandSS.put('H', "Helix");
    shorthandSS.put('E', "Strand");
    shorthandSS.put('C', "Coil");
  }

  // helper function for building links - gets all angles for .aa file
  public void setAngularArray(String content) {
    if (content == null) {
      // aminoAcidError();
      return;
    }
    int n = content.length();
    angles = new Angular[n];
    for (int i = 0; i < n; i++) {
      Angular a = new Angular();
      a.id = i;
      a.aa = content.charAt(i);
      // first and last node have no theta
      if (i == 0 || i == n-1) {
        a.theta = 2 * Math.PI;
      }
      else {
        a.theta = 110.0 * Math.PI / 180.0;
      }
      // first, second to last, and last nodes have no tao
      if (i == 0 || i == n-1 || i == n-2) {
        a.tao = 2 * Math.PI;
      }
      else {
        a.tao = -150.0 * Math.PI / 180.0;
      }
      angles[i] = a;
    }
  }

  public void structureAngularArray(String content) {
    if (content == null || content.length() != links.length) return;
    int n = links.length;
    for (int i = 0; i < angles.length; i++) {
      char c = content.charAt(i);
      angles[i].ss = c;
      if (i != 0 && i != n-1) {
        angles[i].theta = Math.toRadians(secondaryTheta.get(c));
        if (i != n-2) {
          angles[i].tao = Math.toRadians(secondaryTao.get(c));
        }
      }
    }
  }

  public void setLinkArray() {
    int n = carts.length;
    links = new Link[n];
    boolean isEndNode = false;
    for (int i = 0; i < n; i++) {
      Cartesian c = carts[i];
      if (i == carts.length-1) isEndNode = true;
      Link l = new Link(c.ca.x, c.ca.y, c.ca.z, isEndNode);
      l.id = i;
      l.aa = angles[i].aa;
      // null character
      if (angles[i].ss != 0) {
        l.ss = angles[i].ss;
      }
      if (secondaryString == null) {
        l.node.setMaterial(red);
      }
      else {
        switch (secondaryString.charAt(i)) {
          case 'H':
            l.node.setMaterial(red);
            break;
          case 'E':
            l.node.setMaterial(yellow);
            break;
          case 'C':
            l.node.setMaterial(blue);
            break;
        }
      }
      links[i] = l;
    }
  }

  public void connectLinkRods() {
    for (int i = 0; i < links.length-1; i++) {
      links[i].rotateRod(links[i+1]);
    }
  }

  // helper function for building the sequence
  public Link[] buildLinks(String content) {
    setAngularArray(content);
    // convert to cartesion points
    carts = DihedralUtility.angles2Carts(angles);
    setLinkArray();
    connectLinkRods();
    return links;
  }

  public Link[] buildLinks(int selectionIndex) {
    carts = DihedralUtility.angles2Carts(angles);
    setLinkArray();
    connectLinkRods();
    if (selectionIndex != -1) {
      selectedNode = links[selectionIndex].node;
      selectedNode.setMaterial(green);
    }
    return links;
  }

  private String secondaryString;

  public Link[] structureLinks(String content) {
    secondaryString = content;
    structureAngularArray(content);
    carts = DihedralUtility.angles2Carts(angles);
    setLinkArray();
    connectLinkRods();
    return links;
  }

  public void buildSequence() {
    sequence.getChildren().clear();
    world.getChildren().clear();
    sequence.setRotationAxis(Rotate.Y_AXIS);
    sequence.getChildren().addAll(links);
    world.getChildren().add(sequence);
    if (isAutoZoom) setCameraZoom();
  }

  // add all the links
  public void initSequence(File f) throws IOException {
    String content = new Scanner(f).useDelimiter("\n").next();
    content.trim();
    links = buildLinks(content);
    buildSequence();
    setCameraZoom();
  }

  public void applySecondaryStructure(File f) throws IOException {
    String content = new Scanner(f).useDelimiter("\n").next();
    content.trim();
    links = structureLinks(content);
    buildSequence();
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
    // epsilon defined to allow for small precision error in floating point
    thetaSlider.setMin(0 + EPS);
    thetaSlider.setMax(PI - EPS);
    thetaSlider.setValue(PI/2.0);
    thetaSlider.setDisable(true);
    taoSlider.setMin(-PI + EPS);
    taoSlider.setMax(PI - EPS);
    taoSlider.setValue(0);
    taoSlider.setDisable(true);
  }

  // updates a slider with the angle info of a give node
  public void updateSliders(Link l) {
    int id = l.id;
    double theta = angles[id].theta;
    double tao = angles[id].tao;
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

  private void select(Sphere s) {
    if (s == null) return;
    deselect(selectedNode);
    selectedNode = s;
    selectedMaterial = (PhongMaterial) s.getMaterial();
    s.setMaterial(green);
    Link l = (Link) s.getParent();
    updateSliders(l);
    updateLinkLabels(l);
  }

  private void deselect(Sphere s) {
    if (s == null) return;
    s.setMaterial(selectedMaterial);
    selectedNode = null;
    selectedMaterial = null;
    resetSliders();
    resetLinkLabels();
  }

  @FXML private Text idLabel;
  @FXML private Text aaLabel;
  @FXML private Text ssLabel;

  public void updateLinkLabels(Link l) {
    idLabel.setText("Residue ID: " + (l.id + 1));
    aaLabel.setText("Amino Acid: " + shorthandAA.get(l.aa));
    if (l.ss != 0) {
      ssLabel.setText("Secondary Structure: " + shorthandSS.get(l.ss));
    }
  }

  public void resetLinkLabels() {
    idLabel.setText("Residue ID:");
    aaLabel.setText("Amino Acid:");
    ssLabel.setText("Secondary Structure: ");
  }

  private void handle3DMouse(SubScene scene, final Node root) {
    scene.setOnMousePressed(new EventHandler<MouseEvent> () {
      @Override
      public void handle(MouseEvent e) {
        // update mouse position
        mouseXf = e.getSceneX();
        mouseYf = e.getSceneY();
        if (e.isPrimaryButtonDown()) {
          // update selection
          PickResult result = e.getPickResult();
          if (result.getIntersectedNode() instanceof Sphere) {
            select((Sphere) result.getIntersectedNode());
          }
          else {
            deselect(selectedNode);
          }
        }
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
        if (isAutoZoom) return;
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

  public void handleSliders() {
    // track slider value for theta
    thetaSlider.valueProperty().addListener((observable, oldVal, newVal) -> {
      if (selectedNode != null) {
        Link selectedLink = (Link) selectedNode.getParent();
        int index = selectedLink.id;
        double start, end;
        start = oldVal.doubleValue();
        end = newVal.doubleValue();
        if (index != 0 && index != angles.length-1) {
          angles[index].theta = end;
          links = buildLinks(index);
          buildSequence();
        }
      }
    });

    // track undo for theta slider
    thetaSlider.setOnMousePressed((MouseEvent e) -> {
      Link selectedLink = (Link) selectedNode.getParent();
      int id = selectedLink.id;
      history.offerLast(new Undo(id, 'p', thetaSlider.getValue()));
      redo.clear();
    });

    thetaSlider.setOnMouseReleased((MouseEvent e) -> {
      updateScore();
    });

    // track slider change
    taoSlider.valueProperty().addListener((observable, oldVal, newVal) -> {
      if (selectedNode != null) {
        Link head = (Link) selectedNode.getParent();
        int index = head.id;
        double start, end;
        start = oldVal.doubleValue();
        end = newVal.doubleValue();
        if (index != 0 && index != angles.length-2 && index != angles.length-1) {
          angles[index].tao = end;
          links =  buildLinks(index);
          buildSequence();
        }
      }
    });

    // track undo for tao slider
    taoSlider.setOnMousePressed((MouseEvent e) -> {
      Link selectedLink = (Link) selectedNode.getParent();
      int id = selectedLink.id;
      history.offerLast(new Undo(id, 'd', taoSlider.getValue()));
      redo.clear();
    });

    taoSlider.setOnMouseReleased((MouseEvent e) -> {
      updateScore();
    });
  }

  boolean aaFileRead = false;

  // open an amino acid or secondary structure file
  @FXML
  private void openFile(ActionEvent e) throws IOException {
    FileChooser fileModal = new FileChooser();
    fileModal.setTitle("Open Resource File");
    fileModal.getExtensionFilters().addAll(
      new ExtensionFilter("Amino Acid \".aa\"", "*.aa"),
      new ExtensionFilter("Secondary Structure \".ss\"", "*.ss"),
      new ExtensionFilter("Contact Map \".rr\"", "*.rr"),
      new ExtensionFilter("Protein Data Bank \".pdb\"", "*.pdb")
    );
    File f = fileModal.showOpenDialog(app.getScene().getWindow());
    if (f != null) {
      String extension = getExtension(f);
      if (extension.equals(".aa")) {
        aaFileRead = true;
        secondaryString = null;
        initSequence(f);
        if (selectedNode != null) {
          deselect(selectedNode);
        }
      }
      else if (extension.equals(".rr")) {
        generateContactMap(f);
        updateScore();
      }
      else if (aaFileRead && extension.equals(".ss")) {
        applySecondaryStructure(f);
        if (selectedNode != null) {
          deselect(selectedNode);
        }
        updateScore();
      }
    }
  }

  // undo redo fields - implements Queue interface but using as stack
  LinkedList<Undo> history = new LinkedList<Undo>();
  LinkedList<Undo> redo = new LinkedList<Undo>();

  public void undo() {
    Undo u = history.pollLast();
    // no values left in undo history - deselect everything
    if (u == null) {
      if (selectedNode != null) {
        selectedNode.setMaterial(red);
        selectedNode = null;
      }
      return;
    }

    redo.offerLast(u);
    if (u.angleType == 'p') {
      angles[u.id].theta = u.angle;
      links = buildLinks(u.id);
      buildSequence();
      thetaSlider.setValue(u.angle);
      selectedNode = links[u.id].node;
      selectedNode.setMaterial(green);
    }
    else {
      angles[u.id].tao = u.angle;
      links = buildLinks(u.id);
      buildSequence();
      taoSlider.setValue(u.angle);
      selectedNode = links[u.id].node;
      selectedNode.setMaterial(green);
    }
  }

  // CURRENTLY NOT WORKING

  // public void redo() {
  //   Undo r = redo.pollLast();
  //   if (r != null) {
  //     history.offerLast(r);
  //     if (r.angleType == 'p') {
  //       angles[r.id].theta = r.angle;
  //       links = buildLinks(r.id);
  //       buildSequence();
  //       thetaSlider.setValue(r.angle);
  //       selectedNode = links[r.id].node;
  //       selectedNode.setMaterial(green);
  //     }
  //     else {
  //       angles[r.id].tao = r.angle;
  //       links = buildLinks(r.id);
  //       buildSequence();
  //       taoSlider.setValue(r.angle);
  //       selectedNode = links[r.id].node;
  //       selectedNode.setMaterial(green);
  //     }
  //   }
  // }

  // contact map fields
  @FXML private GridPane contactMap;
  @FXML private Text score;

  private boolean[][] isContact;
  private int width;
  private int side;
  private double cellSize;
  private int totalScore;

  public void generateContactMap(File f) throws IOException {
    // add error handling
    if (f == null || links == null) {
      contactMap.getChildren().clear();
      contactMap.getChildren().add(new Rectangle(280, 280, Color.WHITE));
      return;
    }

    width = 240;
    side = links.length;
    cellSize = (double) width / side;
    isContact = new boolean[side][side];
    BufferedReader br = new BufferedReader(new FileReader(f));

    while (true) {
      String line = br.readLine();
      if (line == null) break;
      StringTokenizer st = new StringTokenizer(line);
      int i = Integer.parseInt(st.nextToken()) - 1;
      int j = Integer.parseInt(st.nextToken()) - 1;
      isContact[i][j] = true;
    }
    totalScore = 0;
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        Rectangle r = new Rectangle(cellSize, cellSize);
        if (j >= i && isContact[i][j] || i == j) {
          r.setFill(Color.GREY);
          totalScore += getWeight(i, j);
        }
        else {
          r.setFill(Color.WHITE);
        }
        contactMap.add(r, j, i);
      }
    }
    score.setText("Score: 0 of " + totalScore);
  }

  public int getWeight(int i, int j) {
    int delta = Math.abs(i - j);
    if (delta < 6) return 1;
    if (delta < 12) return 2;
    if (delta < 24) return 4;
    return 8;
  }

  public void updateScore() {
    if (links == null) return;
    int n = links.length;
    boolean[][] tmp = new boolean[n][n];

    int newScore = 0;
    for (int i = 0; i < links.length; i++) {
      for (int j = i+1; j < links.length; j++) {
        if (j == i+1) {
          tmp[j][i] = true;
          continue;
        }

        double distance = getDistance(links[i], links[j]);
        // handle clash
        if (distance < 4.0) {
          undo();
          return;
        }
        if (distance < 8.0) {
          tmp[j][i] = true;
          newScore += getWeight(i, j);
        }
      }
    }
    for (int i = 0; i < n; i++) {
      for (int j = i+1; j < n; j++) {
        if (tmp[j][i]) {
          isContact[j][i] = true;
        }
      }
    }
    updateContactMap(newScore);
  }

  public double getDistance(Link a, Link b) {
    double x0, y0, z0, xF, yF, zF;
    x0 = a.getTranslateX();
    y0 = a.getTranslateY();
    z0 = a.getTranslateZ();
    xF = b.getTranslateX();
    yF = b.getTranslateY();
    zF = b.getTranslateZ();
    double deltaX = xF - x0;
    double deltaY = yF - y0;
    double deltaZ = zF - z0;
    double sum = Math.pow(deltaX, 2) + Math.pow(deltaY, 2) + Math.pow(deltaZ, 2);
    return Math.sqrt(sum);
  }

  public void updateContactMap(int newScore) {
    contactMap.getChildren().clear();
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        Rectangle r = new Rectangle(cellSize, cellSize);
        if (isContact[i][j] || i == j) {
          r.setFill(Color.GREY);
        }
        else {
          r.setFill(Color.WHITE);
        }
        contactMap.add(r, j, i);
      }
    }
    score.setText("Score: " + newScore + " of " + totalScore);
  }

  @FXML
  public void saveToPDB(ActionEvent e) throws IOException {
    if (links == null) return;
    FileChooser fileModal = new FileChooser();
    fileModal.setTitle("Save As...");
    fileModal.getExtensionFilters().add(
      new ExtensionFilter("Protein Data Bank \".pdb\"", "*.pdb")
    );
    File f = fileModal.showSaveDialog(app.getScene().getWindow());
    if (f != null) {
      writeToPDB(f);
    }
  }

  public void writeToPDB(File f) throws IOException {
    BufferedWriter bw = new BufferedWriter(new FileWriter(f));
    carts = DihedralUtility.angles2Carts(angles);
    for (int i = 0; i < links.length; i++) {
      String s = String.format(
        "ATOM %6d  CA  %-3s %5d    %8.3f%8.3f%8.3f  1.00  0.00\n",
        i+1,
        threeCharAA.get(angles[i].aa),
        angles[i].id,
        carts[i].ca.x,
        carts[i].ca.y,
        carts[i].ca.z
      );
      bw.write(s);
    }
    bw.close();
  }

  public void initialize() throws IOException {
    // NOT NEEDED - FOR INTERNAL USE
    // buildAxes();
    initPNGs();
    initShorthandMaps();
    initSecondaryMaps();
    generateContactMap(null);
    resetSliders();
    SubScene view = initView(world);
    Pane viewport = new Pane(view);
    sizeView(view);
    app.setCenter(viewport);
    handle3DMouse(view, world);
    handleSliders();
  }
}
