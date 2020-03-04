#include "ofApp.h"

//--------------------------------------------------------------
void ofApp::setup(){
  ofBackground(0);
  ofSetOrientation(OF_ORIENTATION_DEFAULT, false);

  initialized = false;
  arcore.setup(false);
}

//--------------------------------------------------------------
void ofApp::update(){
  arcore.update();

  // Initialize the projection matrix one time.
  // note that arcore.update() above updates the _view_ matrix, which transforms the projection
  // matrix as needed
  if(!initialized && arcore.isInitialized()){
    initialized = true;
    projectionMatrix = arcore.getProjectionMatrix(0.01f,100.0);
  }

  if(initialized) {
    point_cloud = arcore.getPointCloud(); // get x,y,z,confidence for each point


  }
}

//--------------------------------------------------------------
void ofApp::draw(){
  if(initialized) {
    arcore.draw();

    ofSetMatrixMode(OF_MATRIX_PROJECTION);
    ofLoadMatrix(projectionMatrix);
    ofSetMatrixMode(OF_MATRIX_MODELVIEW);
    ofLoadMatrix(arcore.getViewMatrix());

    // Each point contains x, y, z spatial coords + confidence level
    if (point_cloud.size() >= 1) {
      for (int i = 0; i < point_cloud.size(); i += 4) {
        ofSetColor(ofColor(ofMap(point_cloud[i+3], 0.f, 1.f, 255, 200)));
        ofDrawSphere(point_cloud[i], point_cloud[i + 1], point_cloud[i + 2], 0.01); // Draws a 1cm sphere at each point of point cloud
      }
    }

    point_cloud.clear();
  }
}

//--------------------------------------------------------------
void ofApp::keyPressed  (int key){ 
	
}

//--------------------------------------------------------------
void ofApp::keyReleased(int key){ 
	
}

//--------------------------------------------------------------
void ofApp::windowResized(int w, int h){

}

//--------------------------------------------------------------
void ofApp::touchDown(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::touchMoved(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::touchUp(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::touchDoubleTap(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::touchCancelled(int x, int y, int id){

}

//--------------------------------------------------------------
void ofApp::swipe(ofxAndroidSwipeDir swipeDir, int id){

}

//--------------------------------------------------------------
void ofApp::pause(){

}

//--------------------------------------------------------------
void ofApp::stop(){

}

//--------------------------------------------------------------
void ofApp::resume(){

}

//--------------------------------------------------------------
void ofApp::reloadTextures(){

}

//--------------------------------------------------------------
bool ofApp::backPressed(){
	return false;
}

//--------------------------------------------------------------
void ofApp::okPressed(){

}

//--------------------------------------------------------------
void ofApp::cancelPressed(){

}
