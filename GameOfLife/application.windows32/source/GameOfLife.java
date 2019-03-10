import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class GameOfLife extends PApplet {

//----------------------------------------------------
//yet another Conway's game of life implementation 
//by Tejas Hegde
//It's been implemented slightly differently. placing cells will update a map that maintains the number of surrounding pixels
//this significalntly reduces the number of checks required
//
//the only noteable difference is the way you place cells, althought I'm sure
//that's been done before as well x
//-----------------------------------------------------

//--------Feel free to change these--------
//UI Colors
int backgroundColor = color(0);
int foregroundColor = color(255);
int cursorColor = color(0,255,0);
int cursorPausedColor = color(255,0,0);
final int GRIDSIZE = 700;
//-----------------------------------------

byte[] grid;
float sW = 10;//width of each individual square cell
byte[] surroundingMap;//a speed optimization

//Existing patterns are taken from this github sourcecode -> https://github.com/maniere/Game-of-Life/tree/master/Game-of-Life_full
//I've converted them to coordinate pairs using my own code from elsewhere and put them into a text file because they were causing problems here (error 66355)
//each odd index is an x coordinate, and each even one is a y coordinate, centered around their width and height
//the final 3 integers represent the number of coordinate pairs, the pattern width and the pattern height.
class Pattern{
  int[] points;
  int w,h;
  String name;
}
int currentPatternIndex = 0;
int patternRotation = 0;
boolean flipPattern = false;
Pattern[] patterns;

//----------EXTRACT PATTERN----------

public Pattern extractPattern(String code){
  Pattern p = new Pattern();
  //initialize the name reading from the front
  int index = code.indexOf(',',0);
  p.name = code.substring(0,index);
  
  //get the width, height, and number of points by reading the back of the string
  int endIndex = code.lastIndexOf(',');
  p.h = PApplet.parseInt(code.substring(endIndex+1,code.length()));
  p.w = PApplet.parseInt(code.substring(code.lastIndexOf(',',endIndex-1)+1,endIndex));
  endIndex = code.lastIndexOf(',',endIndex-1);
  int numPoints = PApplet.parseInt(code.substring(code.lastIndexOf(',',endIndex-1)+1, endIndex));
  p.points = new int[numPoints*2];
  
  //get the points themselves by continuing to reading forwards
  for(int i = 0; i < p.points.length; i++){
    int startIndex = index;
    index = code.indexOf(',',index+1);
    String num = code.substring(startIndex+1,index);
    p.points[i] = parseInt(num);
  }
  return p;
}

//----------LOAD PATTERNS----------

public void loadPatterns(){
  String[] lines = loadStrings("patterns.txt");
  patterns = new Pattern[lines.length];
  for(int i = 0; i < lines.length;i++){
    patterns[i] = extractPattern(lines[i]);
  }
}

//----------SETUP----------

public void setup(){
  
  grid = new byte[(GRIDSIZE)*(GRIDSIZE)];
  surroundingMap = new byte[grid.length];
  
  for(int x = 0; x < GRIDSIZE; x++){
    for(int y = 0; y < GRIDSIZE;y++){
      grid[x+GRIDSIZE*y]=0;
    }
  }  
  loadPatterns();
  
  xPos = GRIDSIZE*sW/2;
  yPos = xPos;
  drawState();
  frameRate(120);
}

//simulation speed timing
int frameNo = 0;
int framesPerStep = 1;

//current view
float xPos = 0;
float yPos = 0;
float scale = 1;
float viewSpeed = 5;
float zoomSpeed = 0.1f;
int zoomDir = 0;

//----------ADJUST VIEW----------

public void adjustView(float xAmount, float yAmount, float scaleAmount){
  float sensitivity = 1.0f/scale;
  xPos=constrain(xPos+xAmount*sensitivity,0,GRIDSIZE*sW);
  yPos=constrain(yPos+yAmount*sensitivity,0,GRIDSIZE*sW);
  scale=constrain(scale+scaleAmount*scale,(width-10*sW)/(sW*GRIDSIZE),10);
}

//----------SCREEN TO WORLD SPACE CONVERSION----------

public float toWorldX(float screenX){
  return ((screenX-width/2)/scale)+xPos;
}

public float mouseXPos(){
  return toWorldX(mouseX);
}

public float toWorldY(float screenY){
  return ((screenY-height/2)/scale)+yPos;
}

public float mouseYPos(){
  return toWorldY(mouseY);
}

//----------VOID DRAW----------

public void draw(){
  if(shiftPressed){
    cursor(MOVE);
  } else {
    noCursor();
  }
  
  background(0);
  //change the view
  adjustView(0,0,zoomSpeed*zoomDir);
  if(mousePressed&&shiftPressed){
    float xAmount = mouseX-pmouseX;
    float yAmount = mouseY-pmouseY;
    adjustView(-xAmount,-yAmount,0);
  }
  
  if(useInstructions&&!play){
    fill(cursorPausedColor);
    drawInstructions();
  }
  translate(width/2,height/2);
  scale(scale);
  translate(-xPos,-yPos);
  
  stroke(foregroundColor);
  noFill();
  rect(0,0,GRIDSIZE*sW,GRIDSIZE*sW);
  
  fill(foregroundColor);
  textAlign(CENTER);
  textSize(4*GRIDSIZE/sW);
  text("The Game Of Life on a "+GRIDSIZE+"x"+GRIDSIZE +" wrap enabled grid", GRIDSIZE/2.0f * sW,-2*GRIDSIZE/sW);
  textSize(12);
  
  
  //do simulation every certain amount of frames
  if(frameNo == framesPerStep){
    updateState();
    frameNo = 0;
  } else {
    if(play&&(!(mousePressed&&(mouseButton==LEFT)))){
      frameNo++;
    }
  }
  //draw the state every frame
  fill(foregroundColor);
  noStroke();
  drawState();
  
  //draw brush
  if(play){
    stroke(cursorColor);
  } else {
    stroke(cursorPausedColor);
    fill(cursorPausedColor);
  }

  noFill();
  drawBrush();
}

//----------KEY PRESSED----------

public void keyPressed(){
  switch(keyCode){
    case('b'):
    case('B'):{
      brushType++;
      brushType%=numBrushShapes;
      break;
    } 
    case (' '): {
      play = !play;
      break;
    } 
    case ('.'):
    case ('>'):{
      if(!play){
        updateState();
      }
      break;
    }
    case('I'):
    case('i'):{
      useInstructions=!useInstructions;
      break;
    }
    case('W'):
    case('w'):{
      zoomDir = 1;
      break;
    }
    case('s'):
    case('S'):{
      zoomDir = -1;
      break;
    }
    case('q'):
    case('Q'):{
      currentPatternIndex --;
      if(currentPatternIndex<0){
        currentPatternIndex = patterns.length-1;
      }
      break;
    }
    case('e'):
    case('E'):{
      currentPatternIndex ++;
      if(currentPatternIndex>patterns.length-1){
        currentPatternIndex = 0;
      }
      break;
    }
    case('f'):
    case('F'):{
        flipPattern=!flipPattern;
      break;
    }
    case(SHIFT):{
      shiftPressed=true;
    }
  }
}

//----------KEY RELEASED----------

public void keyReleased(){  
  switch(keyCode){
    case(SHIFT):{
      shiftPressed = false;
    }    
    case('W'):
    case('w'):
    case('s'):
    case('S'):{
      zoomDir = 0;
    }
  }
}

//----------MOUSE WHEEL----------

public void mouseWheel(MouseEvent e){
  if(shiftPressed){
    brushRotation += e.getCount()*PI/60f;
    if(brushType==CUSTOMSHAPES){      
      patternRotation=wrap(patternRotation+1,0,4);
    }
  }else{
    brushRadius=constrain(brushRadius-ceil(scale)*e.getCount(),1,GRIDSIZE);
  }
}

//----------INSTRUCTIONS----------

boolean useInstructions = true;
String[] instructions = {
  "[I] to toggle instructions",
  "Shift+click to pan view",
  "[LMB]/[RMB] to create/kill cells",
  "[B] to change brush",
  "[Mousewheel] to resize brush",
  "[Mousewheel]+[Shift] to rotate brush",
  "[>] to advance 1 step when paused",
  "[W] and [S] to zoom in and out",
  "[Spacebar] to resume and pause the simulation"
};

//----------DRAW INSTRUCTIONS----------

public void drawInstructions(){
  float spacing = 17;
  textAlign(CENTER);
  for(int i = 0; i < instructions.length; i++){
    text(instructions[i],mouseX,mouseY+i*spacing + 100);
  }
}

//----------WORLD TO GRID CONVERSION----------

public int toGridCoord(float c){
  return floor(c/sW);
}

//----------NUMBER SURROUNDING----------

public boolean checkState(byte cellState){
  //return ((cellState&0x0F)==(0x02))||((cellState&0x0F)==(0x01));
  return (cellState==1)||(cellState==2);
}

/* for personal reference:
1 : alive
0 : dead
2 : was alive
3 : was dead
*/

//----------SINGLE SIMULATION STEP----------

/* the rules:
  if a cell has fewer than 2 cells around it, it dies of isolation
  if a cell has 3 living cells aroun it, it comes to life
  if a cell has over 3 living cells around it, it dies of overpopulation
  */
public void doSimulation(int x, int y){
  byte s = surroundingMap[x+(y*GRIDSIZE)];
  int index = x+(GRIDSIZE*y);
  
  if(s < 0x02){
    if(grid[index]==1){
      grid[index] = 2;
    }
  } else if(s==0x03){
    if(grid[index]==0){
      grid[index] = 3;
    }
  } else if(s > 0x03){
    if(grid[index]==1){
      grid[index] = 2;
    }
  }
}

//----------SIMULATE ALL CELLS----------

public void updateState(){
  //apply rules to each cell based on a ruleset
  for(int x = 0; x < GRIDSIZE; x++){
    for(int y = 0; y < GRIDSIZE;y++){
      doSimulation(x,y);
    }
  }
}

//----------TRIVIAL FUNCTIONS----------

public int wrap(int val, int min, int max){
  if (val < min)
      return max - (min - val) % (max - min);
  else
      return min + (val - min) % (max - min);
}

public void setState(float screenX, float screenY, boolean val){
  int x = toGridCoord(screenX);
  int y = toGridCoord(screenY);
  
  setCell(x,y,val); 
}

public void addSurrounding(int x, int y, boolean val){
  int delta = val ? 1 : -1;
  for(int i = -1; i <= 1; i++){
    for(int j = -1; j <= 1; j++){
      if((i==0)&&(j==0)){
        continue;
      }
      
      int x1 = wrap(x+i,0,GRIDSIZE);
      int y1 = wrap(y+j,0,GRIDSIZE);
      
      surroundingMap[x1+(y1*GRIDSIZE)]+=delta;
    }
  }
}

//--------------- SET CELL ------------------

public void setCell(int x, int y,boolean val){
  x=wrap(x,0,GRIDSIZE);
  y=wrap(y,0,GRIDSIZE);
  int index = x+(GRIDSIZE*y);
  if(checkState(grid[index])!=val){
    grid[index] = (val ? PApplet.parseByte(0x01) : PApplet.parseByte(0x00));
    addSurrounding(x,y,val);
  }
}

public byte getCell(int x, int y){
  x=wrap(x,0,GRIDSIZE);
  y=wrap(y,0,GRIDSIZE);
  return grid[x+(GRIDSIZE*y)];
}

public float sqrMagnitude(float x1, float x2){
  return x1*x1+x2*x2;
}

public void drawCellLine(float x1, float y1, float x2, float y2, boolean use, boolean val){
  float len = sqrt(sqrMagnitude(x2-x1,y2-y1));
  float xDir = (x2-x1)/len;
  float yDir = (y2-y1)/len;
  for(float xPos = x1, yPos = y1; sqrMagnitude(x1-xPos,y1-yPos) < len*len;xPos+=xDir,yPos+=yDir){
    drawCell(toGridCoord(xPos),toGridCoord(yPos));
    if(use){
      setState(xPos,yPos,val);
    }
  }
}

//half the program
public void drawBrush(){
  noFill();
  boolean use = mousePressed&&(!shiftPressed);
  boolean val = (mouseButton==LEFT);
  //draw the brush
  switch(brushType){
    case SQUAREBRUSH:{
      if(brushRadius==1){
        drawCell(toGridCoord(mouseXPos()),toGridCoord(mouseYPos()));
        if(use){
          setState(mouseXPos(),mouseYPos(),val);
        }
        break;
      }
      
      float Cos = cos(brushRotation);
      float Sin = sin(brushRotation);
      float x1 = brushRadius*sW*Cos;
      float y1 = brushRadius*sW*Sin;
     
      drawCellLine(mouseXPos()+x1+y1,mouseYPos()+y1-x1,mouseXPos()+y1-x1,mouseYPos()-x1-y1,false,val);
      drawCellLine(mouseXPos()+y1-x1,mouseYPos()-x1-y1,mouseXPos()-x1-y1,mouseYPos()-y1+x1,false,val);
      drawCellLine(mouseXPos()-x1-y1,mouseYPos()-y1+x1,mouseXPos()-y1+x1,mouseYPos()+x1+y1,false,val);
      drawCellLine(mouseXPos()-y1+x1,mouseYPos()+x1+y1,mouseXPos()+x1+y1,mouseYPos()+y1-x1,false,val);
      
      for(int i = -brushRadius; i <= brushRadius; i+= max(brushRadius/5,1)){
        line(mouseXPos()-x1 - Sin*i*sW,mouseYPos()-y1 + Cos*i*sW,mouseXPos()+x1 - Sin*i*sW,mouseYPos()+y1 + Cos*i*sW);
      }
      noStroke();
      for(int i = -brushRadius; i <= brushRadius; i++){
        drawCellLine(mouseXPos()-x1 - Sin*i*sW,mouseYPos()-y1 + Cos*i*sW,mouseXPos()+x1 - Sin*i*sW,mouseYPos()+y1 + Cos*i*sW,use,val);
      }

      break;
    } case LINEBRUSH:{
      float x1 = brushRadius*sW*cos(brushRotation);
      float y1 = brushRadius*sW*sin(brushRotation);
      drawCellLine(x1+mouseXPos(),y1+mouseYPos(),mouseXPos()-x1,mouseYPos()-y1,use,val);
      break;
    } case CIRCLEBRUSH:{
      float dTheta = 1.0f/brushRadius;
      float prevX = brushRadius*sW*cos(-dTheta);
      float prevY = brushRadius*sW*sin(-dTheta);
      for(float theta = 0; theta <= 2.0f*PI;theta+=dTheta){
        float x1 = brushRadius*sW*cos(theta);
        float y1 = brushRadius*sW*sin(theta);
        drawCellLine(mouseXPos()+prevX, mouseYPos()+prevY, mouseXPos()+x1, mouseYPos()+y1,use,val);
        prevX = x1;
        prevY = y1;
      }
      break;
    } case CUSTOMSHAPES:{
      Pattern cur = patterns[currentPatternIndex];
      int patternSize = max(cur.w,cur.h);
      float xLoc = mouseXPos();
      float yLoc = mouseYPos()-(patternSize*sW)/2-sW*3;
      textAlign(CENTER);
      textSize(16);
      text("pattern "+(currentPatternIndex+1)+"/"+patterns.length ,xLoc,yLoc-17);
      text("<-[Q] "+cur.name+" [E]->" ,xLoc,yLoc);
      textAlign(RIGHT);
      text("Shift+mouseWheel to rotate", xLoc-(patternSize+2)*sW/2.0f, mouseYPos());
      textAlign(LEFT);
      text("[F]lip", xLoc+(patternSize+2)*sW/2.0f, mouseYPos());
      textSize(12);
      
      for(int i = 0; i < cur.points.length-1;i+=2){
        //draw the cells
        int xOff = cur.points[i];
        int yOff = cur.points[i+1];
        int xOffs[] = {xOff,yOff,-xOff,-yOff};
        int yOffs[] = {yOff,-xOff,-yOff,xOff};
        xOff = xOffs[patternRotation];
        yOff = yOffs[patternRotation];
        if(flipPattern)
          xOff = -xOff;
          
        int xLoc2 = toGridCoord(mouseXPos())+xOff;  
        int yLoc2 = toGridCoord(mouseYPos())-yOff;
        drawCell(xLoc2, yLoc2);
        //set the state if being used
        if(use){
          setCell(xLoc2,yLoc2,val);
        }
      }
      break;
    }
  }
}

int brushType = 0;
int brushRadius = 1;
float brushRotation = 0;

final int SQUAREBRUSH = 0;
final int LINEBRUSH = 1;
final int CIRCLEBRUSH = 2;
final int CUSTOMSHAPES = 3;
final int numBrushShapes = 4;

boolean play = false;
boolean shiftPressed = false;

public void drawCell(int x, int y){
  rect(x*sW-1,y*sW-1,sW+1,sW+1);
}

public void drawState(){
  fill(foregroundColor);
  stroke(foregroundColor);

  for(int x = 0; x < GRIDSIZE; x++){
    for(int y = 0; y < GRIDSIZE;y++){
      int index = x+(GRIDSIZE*y);
      if(grid[index]==0)
        continue;
      
      if(grid[index]==2){
        setCell(x,y,false);
      } else if (grid[index]==3){
        setCell(x,y,true);
      }
      
      if(grid[index]==1){
        drawCell(x,y);
      }
    }
  }
}
  public void settings() {  size(700,700); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "GameOfLife" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
