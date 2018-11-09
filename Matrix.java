import java.io.*;
import java.util.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import javafx.scene.Node;
import javafx.scene.shape.*;
import javafx.geometry.Insets;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;


public class Matrix extends Application{

   Stage stage;

   public static void main(String[] args) {
      launch(args);
   }
   
   @Override
   public void start(Stage primaryStage) throws Exception {
      stage = primaryStage;
      stage.setTitle("Test");
      
      File inputFile = new File("/Users/kaeul/1AIL/1ail.rr");
      Scene scene = new Scene(generateMatrix(inputFile), 420, 420);
      stage.setScene(scene);
      
      stage.show();
   }


   public Pane generateMatrix(File file) throws Exception {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      double height = screenSize.getHeight() / 2;
      Pane contactMatrix = new Pane();
               
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      
      ArrayList<Integer> iPoints = new ArrayList<Integer>();
      ArrayList<Integer> jPoints = new ArrayList<Integer>();
      
      //read in file and populate i and j arraylists 
      while((line = br.readLine()) != null) {   
         String iPoint = line.substring(0, line.indexOf(' '));
         int iPointValue = Integer.parseInt(iPoint);
         
         String newString = line.substring(line.indexOf(' '));
         newString = newString.trim();
         String jPoint = newString.substring(0, newString.indexOf(' '));
         int jPointValue = Integer.parseInt(jPoint);
         
         iPoints.add(iPointValue);
         jPoints.add(jPointValue); 
      }
      
      int numSpheres = getMax(jPoints);
     
      //create cells for contact matrix
      Rectangle [][] grid = new Rectangle [numSpheres][numSpheres];
      int cellSize = (int) Math.round(height / numSpheres);
   
      for(int i = 0; i < numSpheres; i++) {
         for (int j = 0; j < numSpheres; j++) {
            grid[i][j] = new Rectangle();
            grid[i][j].setX(i * cellSize);
            grid[i][j].setY(j * cellSize);
            grid[i][j].setWidth(cellSize);
            grid[i][j].setHeight(cellSize);
            grid[i][j].setFill(null);
            grid[i][j].setStroke(Color.BLACK);
            
            if (i == j) {
               grid[i][j].setFill(Color.BLACK);
            }
            contactMatrix.getChildren().add(grid[i][j]);
         }
      }
     
     //fill cells that are in contact
      for(int i = 0; i < jPoints.size(); i++) {
         int iValue = iPoints.get(i);
         int jValue = jPoints.get(i);
         grid[jValue - 1][iValue - 1].setFill(Color.GREEN);
      }
      return contactMatrix;
   }
   
   //return number of spheres in molecule
   //FIND BETTER WAY TO DO THIS
   public int getMax(ArrayList<Integer> intArray) {
      int max = 0;
      for (int i = 0; i < intArray.size() - 1; i++) {
         max = intArray.get(i);
         if (intArray.get(i + 1) > max) {
            max = intArray.get(i + 1); 
         }
      }
      return max;
   }
   
   public double getEuclideanDistance(Sphere point1, Sphere point2){
      double firstPointXValue = point1.getTranslateX();
      double firstPointYValue = point1.getTranslateY();
      double firstPointZValue = point1.getTranslateZ();
      
      double secondPointXValue = point2.getTranslateX();
      double secondPointYValue = point2.getTranslateY();
      double secondPointZValue = point2.getTranslateZ();
      
      double xCalc = Math.pow((firstPointXValue - secondPointXValue), 2);
      double yCalc = Math.pow((firstPointYValue - secondPointYValue), 2);
      double zCalc = Math.pow((firstPointZValue - secondPointZValue), 2);
      
      double distance = Math.sqrt(xCalc + yCalc + zCalc);
   
      return distance;
   }
}