//----------------------------------------------------
//yet another Conway's game of life implementation 
//by Tejas Hegde
//They will be commented as such
//-----------------------------------------------------

//UI Colors
color backgroundColor = color(0);
color foregroundColor = color(255);
color cursorColor = color(0,255,0);
color cursorPausedColor = color(255,0,0);

//cell grid
final int GRIDSIZE = 500;
char[][] grid;//I made it a square for simplicity's sake
float sW;//squareWidth

int currentPatternIndex = 0;
//Existing patterns are taken from maniere's github sourcecode -> https://github.com/maniere/Game-of-Life/tree/master/Game-of-Life_full
//I've converted them to coordinate pairs using my own code from elsewhere and put them into a text file because they were causing problems here (error 66355)
//each odd index is an x coordinate, and each even one is a y coordinate, centered around their width and height
//the final two xy coordinates represent a width and a height. I decided to leave them in since they might be useful
class Pattern{
  int[] points;
  int w,h;
  String name;
}

Pattern[] patterns;

Pattern extractPattern(String code){
  Pattern p = new Pattern();
  //initialize the name
  int index = code.indexOf(',',0);
  p.name = code.substring(0,index);
  
  //get the number of points, and their widths and heights
  int endIndex = code.lastIndexOf(',');
  p.h = int(code.substring(endIndex+1,code.length()));
  p.w = int(code.substring(code.lastIndexOf(',',endIndex-1)+1,endIndex));
  endIndex = code.lastIndexOf(',',endIndex-1);
  int numPoints = int(code.substring(code.lastIndexOf(',',endIndex-1)+1, endIndex));
  p.points = new int[numPoints*2];
  
  //get the points
  for(int i = 0; i < p.points.length; i++){
    int startIndex = index;
    index = code.indexOf(',',index+1);
    String num = code.substring(startIndex+1,index);
    println(num);
    p.points[i] = parseInt(num);
  }
  return p;
}

void loadPatterns(){
  String[] lines = loadStrings("patterns.txt");
  patterns = new Pattern[lines.length];
  for(int i = 0; i < lines.length;i++){
    patterns[i] = extractPattern(lines[i]);
  }
}

void setup(){
  size(700,700);
  grid = new char[GRIDSIZE][GRIDSIZE];
  sW = 10;
  for(int x = 0; x < grid.length; x++){
    for(int y = 0; y < grid[0].length;y++){
      grid[x][y] = '0';
    }
  }
  textSize(16);
  textAlign(CENTER);
  
  loadPatterns();
  
  xPos = GRIDSIZE*sW/2;
  yPos = xPos;
  drawState();
}

//simulation speed timing
int frameNo = 0;
int framesPerStep = 1;

//current view
float xPos = 0;
float yPos = 0;
float scale = 1;
float viewSpeed = 5;
float zoomSpeed = 0.1;
int zoomDir = 0;

void adjustView(float xAmount, float yAmount, float scaleAmount){
  float sensitivity = 1.0/scale;
  xPos=constrain(xPos+xAmount*sensitivity,0,GRIDSIZE*sW);
  yPos=constrain(yPos+yAmount*sensitivity,0,GRIDSIZE*sW);
  scale=constrain(scale+scaleAmount*scale,0.1,10);
}

//vital helper functions
float toWorldX(float screenX){
  return ((screenX-width/2)/scale)+xPos;
}

float mouseXPos(){
  return toWorldX(mouseX);
}

float toWorldY(float screenY){
  return ((screenY-height/2)/scale)+yPos;
}

float mouseYPos(){
  return toWorldY(mouseY);
}

void draw(){
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
  
  noFill();
  stroke(foregroundColor);
  rect(0,0,GRIDSIZE*sW,GRIDSIZE*sW);
  
  //do simulation every certain amount of frames
  if(frameNo == framesPerStep){
    updateState();
    frameNo = 0;
  } else {
    if(play){
      frameNo++;
    }
  }
  //draw the state every frame
  stroke(backgroundColor);
  drawState();
  if(play){
    stroke(cursorColor);
  } else {
    stroke(cursorPausedColor);
    fill(cursorPausedColor);
  }

  noFill();
  drawBrush();
}

void keyPressed(){
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
    case(SHIFT):{
      shiftPressed=true;
    }
  }
}

void mouseWheel(MouseEvent e){
  if(shiftPressed){
    brushRotation += e.getCount()*PI/60f;
  }else{
    brushRadius=constrain(brushRadius-ceil(scale)*e.getCount(),1,GRIDSIZE);
  }
}

void keyReleased(){  
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

void drawInstructions(){
  float spacing = 17;
  for(int i = 0; i < instructions.length; i++){
    text(instructions[i],mouseX,mouseY+i*spacing + 100);
  }
}

int toGridCoord(float c){
  return floor(c/sW);
}

int numSurrounding(int x, int y){
  int sum = 0;
  for(int i = -1; i <= 1; i++){
    for(int j = -1; j <= 1; j++){
      if((i==0)&&(j==0)){
        continue;
      }
      
      char state = getCell(x+i,y+j);
      if(state=='1'||state=='2'){
        sum++;
      }
    }
  }
  return sum;
}

/* for personal reference:
1 : alive
0 : dead
2 : was alive
3 : was dead
*/

void doSimulation(int x, int y){
  int s = numSurrounding(x,y);
  /* Conway's rules
  if a cell has fewer than 2 cells around it, it dies of isolation
  if a cell has 3 living cells aroun it, it comes to life
  if a cell has over 3 living cells around it, it dies of overpopulation
  */
  if(s < 2){
    if(grid[x][y]=='1'){
      grid[x][y] = '2';
    }
  } else if((s==2)||(s==3)){
    if(s==3){
      if(grid[x][y]=='0'){
        grid[x][y] = '3';
      }
    }
  } else if(s > 3){
    if(grid[x][y]=='1'){
      grid[x][y] = '2';
    }
  }
}

void updateState(){
  //apply rules to each cell based on a ruleset
  for(int x = 0; x < grid.length; x++){
    for(int y = 0; y < grid[0].length;y++){
      doSimulation(x,y);
    }
  }
  
  //collapse the in-between states like 2 and 3 to 1 and 0
  for(int x = 0; x < grid.length; x++){
    for(int y = 0; y < grid[0].length;y++){
      if(grid[x][y]=='2'){
        grid[x][y]='0';
      } else if (grid[x][y]=='3'){
        grid[x][y]='1';
      }
    }
  }
}

int wrap(int val, int min, int max){
  if(val > max){
    return abs(val-max)%abs(min-max)+min;
  } else if (val < min){
    return max - abs(val-min)%abs(min-max);
  } else {
    return val;
  }
}

void setState(float screenX, float screenY, boolean val){
  int x = toGridCoord(screenX);
  int y = toGridCoord(screenY);
  
  setCell(x,y,val); 
}

void setCell(int x, int y,boolean val){
  x=wrap(x,0,GRIDSIZE-1);
  y=wrap(y,0,GRIDSIZE-1);
  grid[x][y] = val ? '1' : '0';
}

char getCell(int x, int y){
  x=wrap(x,0,GRIDSIZE-1);
  y=wrap(y,0,GRIDSIZE-1);
  return grid[x][y];
}

float sqrMagnitude(float x1, float x2){
  return x1*x1+x2*x2;
}

void drawCellLine(float x1, float y1, float x2, float y2, boolean use, boolean val){
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
void drawBrush(){
  noFill();
  boolean use = mousePressed&&(!shiftPressed);
  boolean val = (mouseButton==LEFT);
  //draw the brush
  switch(brushType){
    case XBRUSH:{
      drawCell(toGridCoord(mouseXPos()),toGridCoord(mouseYPos()));
      if(use){
        setState(mouseXPos(), mouseYPos(), val);
      }
      break;
    } case LINEBRUSH:{
      float x1 = brushRadius*sW*cos(brushRotation);
      float y1 = brushRadius*sW*sin(brushRotation);
      drawCellLine(x1+mouseXPos(),y1+mouseYPos(),mouseXPos()-x1,mouseYPos()-y1,use,val);
      break;
    } case SQUAREBRUSH:{
      float Cos = cos(brushRotation);
      float Sin = sin(brushRotation);
      float x1 = brushRadius*sW*Cos;
      float y1 = brushRadius*sW*Sin;
      drawCellLine(mouseXPos()+x1+y1,mouseYPos()+y1-x1,mouseXPos()+y1-x1,mouseYPos()-x1-y1,use,val);
      drawCellLine(mouseXPos()+y1-x1,mouseYPos()-x1-y1,mouseXPos()-x1-y1,mouseYPos()-y1+x1,use,val);
      drawCellLine(mouseXPos()-x1-y1,mouseYPos()-y1+x1,mouseXPos()-y1+x1,mouseYPos()+x1+y1,use,val);
      drawCellLine(mouseXPos()-y1+x1,mouseYPos()+x1+y1,mouseXPos()+x1+y1,mouseYPos()+y1-x1,use,val);
      break;
    } case CIRCLEBRUSH:{
      float dTheta = 1.0/brushRadius;
      float prevX = brushRadius*sW*cos(-dTheta);
      float prevY = brushRadius*sW*sin(-dTheta);
      for(float theta = 0; theta <= 2.0*PI;theta+=dTheta){
        float x1 = brushRadius*sW*cos(theta);
        float y1 = brushRadius*sW*sin(theta);
        drawCellLine(mouseXPos()+prevX, mouseYPos()+prevY, mouseXPos()+x1, mouseYPos()+y1,use,val);
        prevX = x1;
        prevY = y1;
      }
      break;
    } case CUSTOMSHAPES:{
      Pattern cur = patterns[currentPatternIndex];
      float xLoc = mouseXPos();
      float yLoc = mouseYPos()-(cur.h*sW)/2-sW*3;
      textAlign(CENTER);
      if(!play){
        text("pattern "+(currentPatternIndex+1)+"/"+patterns.length ,xLoc,yLoc-17);
        text("<-[Q] "+cur.name+" [E]->" ,xLoc,yLoc);
      } else {
        text("Pause the simulation for this to place properly" ,xLoc,yLoc);
      }
      
      for(int i = 0; i < cur.points.length-1;i+=2){
        int xLoc2 = toGridCoord(mouseXPos())+cur.points[i];
        int yLoc2 = toGridCoord(mouseYPos())-cur.points[i+1];
        drawCell(xLoc2, yLoc2);
        if(use){
          setCell(xLoc2,yLoc2,val);
        }
      }
      break;
    }
  }
}

int brushType = 0;
int brushRadius = 5;
float brushRotation = 0;

final int XBRUSH = 0;
final int LINEBRUSH = 1;
final int SQUAREBRUSH = 2;
final int CIRCLEBRUSH = 3;
final int CUSTOMSHAPES = 4;
final int numBrushShapes = 5;

boolean play = false;
boolean shiftPressed = false;

void drawCell(int x, int y){
  rect(x*sW,y*sW,sW,sW);
}

void drawState(){
  fill(foregroundColor);
  stroke(foregroundColor);
  //draw grid
  for(int x = 0; x < grid.length; x++){
    for(int y = 0; y < grid[0].length;y++){      
      if(grid[x][y]=='1'){
        drawCell(x,y);
      }
    }
  }
}