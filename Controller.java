import javafx.event.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.transform.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

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
    view.setFill(Color.web("1a1a1a"));
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
    double tanTheta = Math.tan(PI / 6.0);
    // keep track of max zoom level needed to see all nodes
    double maxZoom, maxLeft, maxRight, maxUp, maxDown;
    maxZoom = -1;
    // initially set with very high values in opposite direction
    maxLeft = INF;
    maxRight = -INF;
    maxUp = -INF;
    maxDown = INF;

    // finds max left, max right, max up, and max down to calculate camera field of view
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
        a.theta = 2 * PI;
      }
      else {
        a.theta = 110.0 * PI / 180.0;
      }
      // first, second to last, and last nodes have no tao
      if (i == 0 || i == n-1 || i == n-2) {
        a.tao = 2 * PI;
      }
      else {
        a.tao = -150.0 * PI / 180.0;
      }
      angles[i] = a;
    }
  }

  public void structureAngularArray(String content) {
    // .ss has different number of characters than there are links
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
    // 
    boolean isEndNode = false;
    for (int i = 0; i < n; i++) {
      //System.out.println(i);
      Cartesian c = carts[i];
      if (i == carts.length-1) isEndNode = true;
      // link constructor sets rod length to 0 if isEndNode
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
    //carts = Cartesian[];
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
    // no extension
    if (index == -1) return null;
    return name.substring(index);
  }

  public String getBaseName(File f) {
    String name = f.getName();
    int index = name.lastIndexOf(".");
    if (index == -1) return null;
    return name.substring(0, index);
  }

  // UI
  @FXML private Slider thetaSlider;
  @FXML private Slider taoSlider;
  @FXML private ImageView piPNG1;
  @FXML private ImageView piPNG2;
  @FXML private ImageView negPiPNG;
  @FXML private ImageView zeroPNG;
  @FXML private ProgressBar progressBar;

  private final double PI = Math.PI;

  // disables sliders and puts the selector halfway
  public void resetSliders() {
    // epsilon defined to allow for small error in floating point precision
    thetaSlider.setMin(0 + EPS);
    thetaSlider.setMax(PI - EPS);
    thetaSlider.setValue(PI / 2.0);
    thetaSlider.setDisable(true);
    taoSlider.setMin(-PI + EPS);
    taoSlider.setMax(PI - EPS);
    taoSlider.setValue(0);
    taoSlider.setDisable(true);
    thetaDegreeText.setText("");
    taoDegreeText.setText("");
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

  int indexSelected;
  @FXML private Text thetaDegreeText; 
  @FXML private Text taoDegreeText;

  public void handleSliders() {
    // track slider value for theta
    DecimalFormat df = new DecimalFormat("0.0");
    thetaSlider.valueProperty().addListener((observable, oldVal, newVal) -> {
      updateScore();
      if (selectedNode != null) {
        Link selectedLink = (Link) selectedNode.getParent();
        int index = selectedLink.id;
        indexSelected = index;
        double start, end;
        start = oldVal.doubleValue();
        end = newVal.doubleValue();
        if (index != 0 && index != angles.length-1) {
          angles[index].theta = end;
          thetaDegreeText.setText(df.format(thetaSlider.getValue() * 180 / PI) + "\u00b0");
          links = buildLinks(index);
          buildSequence();
        }
      }
    });

    // track undo for theta slider
    thetaSlider.setOnMousePressed((MouseEvent e) -> {
      Link selectedLink = (Link) selectedNode.getParent();
      int id = selectedLink.id;
      undoStack.offerLast(new Undo(id, 'p', thetaSlider.getValue()));
      redoStack.clear();
    });

    // TODO: try to get this in real time
    thetaSlider.setOnMouseReleased((MouseEvent e) -> {
      updateScore();
      checkClash();
    });

    // track slider change
    taoSlider.valueProperty().addListener((observable, oldVal, newVal) -> {
      if (selectedNode != null) {
        updateScore();
        Link selectedLink = (Link) selectedNode.getParent();
        int index = selectedLink.id;
        double start, end;
        start = oldVal.doubleValue();
        end = newVal.doubleValue();
        if (index != 0 && index != angles.length-2 && index != angles.length-1) {
          angles[index].tao = end;
          taoDegreeText.setText(df.format(taoSlider.getValue() * 180 / PI) + "\u00b0");
          links =  buildLinks(index);
          buildSequence();
        }
      }
    });

    // track undo for tao slider
    taoSlider.setOnMousePressed((MouseEvent e) -> {
      Link selectedLink = (Link) selectedNode.getParent();
      int id = selectedLink.id;
      undoStack.offerLast(new Undo(id, 'd', taoSlider.getValue()));
      redoStack.clear();
    });

    taoSlider.setOnMouseReleased((MouseEvent e) -> {
      updateScore();
      checkClash();
    });
  }

  @FXML
  private void minimize(ActionEvent e) {
    PolyFold.minimizeWindow();
  }

  @FXML
  private void fullScreen(ActionEvent e) {
    PolyFold.makeFullScreen();
    switchFull();
  }
  
  boolean distanceMapLoaded = false;

  @FXML
  private void openFile(ActionEvent e) throws IOException {
    FileChooser fileModal = new FileChooser();
    fileModal.setTitle("Open Resource File");
    fileModal.getExtensionFilters().addAll(
      new ExtensionFilter("ZIP Archive \".zip\"", "*.zip")
    );

    File f = fileModal.showOpenDialog(app.getScene().getWindow());
    LinkedList<File> filesInZip = new LinkedList<File>();
    if (f != null) {
      String extension = getExtension(f);
      if (extension.equals(".zip")) {
        filesInZip = unzip(f);
      }
      String proteinName = checkFileProteinMatch(filesInZip);
      File[] fileTypes = detectFileTypes(filesInZip, proteinName);
      if (proteinName != null) {
        File aa = fileTypes[0];
        File ss = fileTypes[1];
        File pdb = fileTypes[2];
        initSequence(aa);
        applySecondaryStructure(ss);
        generateDistanceMap(pdb);
        PolyFold.setPrimaryStageTitle("PoltFold (Alpha Version) " + proteinName);
        return;
      }
      // error dialog
    }
  }

  public String checkFileProteinMatch(LinkedList<File> fileTypes) {
    String proteinName;
    for (File file : fileTypes) {
      if (getExtension(file).equals(".aa")) {
        return(getBaseName(file));
      }
    }
    return null;
  }

  public File[] detectFileTypes(LinkedList<File> files, String proteinName) {
    // 0 - .aa, 1 -.ss, 2 -.pdb
    File[] fileTypes= new File[3];
    for (File file : files) {
      String baseName = getBaseName(file);
      String extension = getExtension(file);
      if (extension.equals(".aa") && baseName.equals(proteinName)) {
        if (fileTypes[0] != null) return null;
        fileTypes[0] = file;
      }
      else if (extension.equals(".ss") && baseName.equals(proteinName)) {
        if (fileTypes[1] != null) return null;
        fileTypes[1] = file;
      }
      else {
        if (fileTypes[2] != null && baseName.equals(proteinName)) return null;
        fileTypes[2] = file;
      }
    }
    return fileTypes;
  }

  public LinkedList<File> unzip(File zip) throws IOException {
    LinkedList<File> extracted = new LinkedList<File>();
    byte[] buffer = new byte[1024];
    String directory = createOutputDirectory(zip);

    ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
    while (true) {
      ZipEntry ze = zis.getNextEntry();
      if (ze == null) break;
      if (!ze.getName().matches("[-_. A-Za-z0-9]+\\.(aa|ss|pdb)")) {
        continue;
      }
      String fileName = ze.getName();
      File f = new File(directory + File.separator + fileName);
      extracted.offer(f);
      FileOutputStream fos = new FileOutputStream(f);
      int bytesWritten;
      while ((bytesWritten = zis.read(buffer)) > 0) {
        fos.write(buffer, 0, bytesWritten);
      }  
      fos.close();
    }
    zis.close();
    for (File file : extracted) {
    }
    return extracted;
  }

  public String createOutputDirectory(File zip) throws IOException {
    String directory = getBaseName(zip);
    File f = new File(directory);
    if (!f.exists()) {
      f.mkdir();
    }
    return f.getCanonicalPath();
  }   


  // undo redo fields - implements Queue interface but using as stack
  LinkedList<Undo> undoStack = new LinkedList<Undo>();
  LinkedList<Undo> redoStack = new LinkedList<Undo>();

  @FXML
  public void undo() {
    Undo u = undoStack.pollLast();
    // no values left in undo undoStack - deselect everything
    if (u == null) {
      if (selectedNode != null) {
        selectedNode.setMaterial(red);
        selectedNode = null;
      }
      return;
    }
    if (u.angleType == 'p') {
      redoStack.offerLast(new Undo(indexSelected, 'p', thetaSlider.getValue()));
      thetaSlider.setValue(u.angle);
      angles[u.id].theta = u.angle;
      links = buildLinks(u.id);
      buildSequence();
      selectedNode = links[u.id].node;
      indexSelected = u.id;
      selectedNode.setMaterial(green);
    }
    else {
      redoStack.offerLast(new Undo(indexSelected, 'p', taoSlider.getValue()));
      angles[u.id].tao = u.angle;
      links = buildLinks(u.id);
      buildSequence();
      taoSlider.setValue(u.angle);
      selectedNode = links[u.id].node;
      selectedNode.setMaterial(green);
    }
    updateScore();
  }

  // CURRENTLY WORKING

  public void redo() {
    Undo r = redoStack.pollLast();
    if (r != null) {
      undoStack.offerLast(r);
      if (r.angleType == 'p') {
        angles[r.id].theta = r.angle;
        links = buildLinks(r.id);
        buildSequence();
        thetaSlider.setValue(r.angle);
        selectedNode = links[r.id].node;
        selectedNode.setMaterial(green);
        undoStack.offerLast(new Undo(r.id, 'p', thetaSlider.getValue()));
      }
      else {
        angles[r.id].tao = r.angle;
        links = buildLinks(r.id);
        buildSequence();
        taoSlider.setValue(r.angle);
        selectedNode = links[r.id].node;
        selectedNode.setMaterial(green);
        undoStack.offerLast(new Undo(r.id, 'd', taoSlider.getValue()));
      }
    }
  }

  public double[] parsePDBLine(String line) {
    double[] coord = new double[3];
    // x, y, z
    coord[0] = Double.parseDouble(line.substring(31, 38).trim());
    coord[1] = Double.parseDouble(line.substring(39, 46).trim());
    coord[2] = Double.parseDouble(line.substring(47, 54).trim());
    return coord;
  }

  // contact map fields
  @FXML private GridPane distanceMap;
  @FXML private Text score;

  private double[][] distanceArray;
  private int side;
  private double cellSize;
  private int totalScore;
  private double maxDistanceAA; 
  private double[][] pdbExpectedCoords;

  public void generateDistanceMap(File f) throws IOException {
    // add error handling
    distanceMap.getChildren().clear();
    if (f == null || links == null) {
      distanceMap.getChildren().add(new Rectangle(280, 280, Color.WHITE));
      return;
    }
    int width = 240;
    side = links.length;
    cellSize = (double) width / side;
    distanceArray = new double[side][side];
    readPDB(f);
    double totalScore = 0.0;
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        double dist;
        if (i < j) {
          dist = getDistance(pdbExpectedCoords[i], pdbExpectedCoords[j]);
        }
        else if (i > j) {
          dist = getDistance(links[i], links[j]);
        }
        else {
          // identity pairs
          dist = -1;
        }
        distanceArray[i][j] = dist;
      }
    }
    buildDistanceMap();
    updateScore();
  }

  public void switchFull() {
    int width;
    if(PolyFold.checkFullScreen()) {
      width = 340;
    }
    else {
      width = 240;
    }
    cellSize = (double) width / side;
    if(rects[0][0].getWidth() - cellSize > EPS) return;
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        rects[i][j].setWidth(cellSize);
        rects[i][j].setHeight(cellSize);
      }
    }
    updateScore();
  }

  public void readPDB(File f) throws IOException {
    pdbExpectedCoords = new double[links.length][3];
    BufferedReader br = new BufferedReader(new FileReader(f));
    for (int i = 0; i < links.length; i++) {
      String line = br.readLine();
      double[] pdbInfo = parsePDBLine(line);
      for (int j = 0; j < 3; j++) {
        pdbExpectedCoords[i][j] = pdbInfo[j];
      }
    }
  }

  Rectangle[][] rects;

  public void buildDistanceMap() {
    rects = new Rectangle[side][side];
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        Rectangle r = new Rectangle(cellSize, cellSize);
        double colorPercent;
        if (i < j) {
          r.setFill(Color.rgb(0, 255, 0));
        }
        else if (i > j) {
          double expected = distanceArray[j][i];
          double actual = distanceArray[i][j];
          if (actual <= expected) {
            colorPercent = actual / expected;
          }
          else {
            double maxDistanceAA = 3.8 * Math.abs(i - j);
            colorPercent = Math.abs(actual - expected) / (maxDistanceAA - expected);
          }
          
          int greenValue = (int) (colorPercent * 510);
          int redValue = Math.min(255, 510 - greenValue);
          greenValue = Math.min(255, greenValue);
          r.setFill(Color.rgb(redValue, greenValue, 0));
        }
        else {
          r.setFill(Color.rgb(0, 0, 0));
        }
        distanceMap.add(r, j, i);
        rects[i][j] = r;
      }
    }
    distanceMapLoaded = true;
    getMaxMeanSquaredError();
  }

  public int getWeight(int i, int j) {
    int delta = Math.abs(i - j);
    if (delta < 6) return 1;
    if (delta < 12) return 2;
    if (delta < 24) return 4;
    return 8;
  }

  private double maxMeanSquaredError;
  // divide the score of it by the multiples it is from it
  public void updateScore() {
    if (links == null) return;
    if (!distanceMapLoaded) return;
    double mse = getMeanSquaredError();
    progressBar.setProgress(mse / maxMeanSquaredError);
    updateDistanceMap(mse);
  }

  public void getMaxMeanSquaredError() {
    double mse = 0;
    int denom = 0;
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        if (i > j) continue;
        if (i == j) {
          denom++;
          continue;
        }
        int range = Math.abs(i-j);
        double expected = distanceArray[i][j];
        double maxDistance = 3.8 * range;
        double maxError = Math.max(maxDistance - expected, expected);
        mse += Math.pow(maxError, 2) * getWeight(i, j);
        denom++;
      }
    }
    maxMeanSquaredError = (int) (mse / denom);
  }

  public double getMeanSquaredError() {
    double mse = 0;
    int denom = 0;
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        if (i < j) continue;
        if (i == j) {
          denom++;
          continue;
        }
        int range = Math.abs(i-j);
        double actual = distanceArray[i][j];
        double expected = distanceArray[j][i];
        double error = Math.abs(actual - expected);
        mse += Math.pow(error, 2) * getWeight(i, j);
        denom++;
      }
    }
    return mse / denom;
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

  public double getDistance(double[] a, double[] b) {
    double x0, y0, z0, xF, yF, zF;
    x0 = a[0];
    y0 = a[1];
    z0 = a[2];
    xF = b[0];
    yF = b[1];
    zF = b[2];
    double deltaX = xF - x0;
    double deltaY = yF - y0;
    double deltaZ = zF - z0;
    double sum = Math.pow(deltaX, 2) + Math.pow(deltaY, 2) + Math.pow(deltaZ, 2);
    return Math.sqrt(sum);
  }

  public void updateDistanceMap(double mse) {
    // distanceMap.getChildren().clear();
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        if (i > j) {
          distanceArray[i][j] = getDistance(links[i], links[j]);
        }
      }
    }
    updateMapColors();
    score.setText(" " + (int) mse + " / " + (int) maxMeanSquaredError);
  }

  public void updateMapColors() {
    for (int i = 1; i < side; i++) {
      for (int j = 0; j < i; j++) {
      // only run on bottom triangle
        if ((i < indexSelected && j < indexSelected) 
          || (i > indexSelected && j > indexSelected)) continue;
          // if they are both on the same side of the moving node, don't recalculate
        double colorPercent;
        double expected = distanceArray[j][i];
        double actual = distanceArray[i][j];
        if (actual <= expected) {
          colorPercent = actual / expected;
        }
        else {
          double maxDistanceAA = 3.8 * Math.abs(i - j);
          colorPercent = Math.abs(actual - expected) / (maxDistanceAA - expected);
        }
        int greenValue = (int) (colorPercent * 510);
        int redValue = Math.min(255, 510 - greenValue);
        greenValue = Math.min(255, greenValue);
        rects[i][j].setFill(Color.rgb(redValue, greenValue, 0));
      }
    }
  }

  public void checkClash() {
    for (int i = 0; i < side; i++) {
      for (int j = 0; j < side; j++) {
        if (i <= j) continue;
        if (distanceArray[i][j] < 1.8) {
          undo();
          undoStack.pollLast();
        }
      }
    }
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
    generateDistanceMap(null);
    resetSliders();
    SubScene view = initView(world);
    Pane viewport = new Pane(view);
    sizeView(view);
    app.setCenter(viewport);
    handle3DMouse(view, world);
    handleSliders();
  }
}