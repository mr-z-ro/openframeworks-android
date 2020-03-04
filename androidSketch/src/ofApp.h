#pragma once

#include "ofMain.h"
#include "ofxAndroid.h"
#include "ofxARCore.h"

class ofApp : public ofxAndroidApp{
	
	public:

		ofxARCore arcore;
		bool initialized;

		ofMatrix4x4 projectionMatrix;

	    vector<float> point_cloud;
		
		void setup();
		void update();
		void draw();
		
		void keyPressed(int key);
		void keyReleased(int key);
		void windowResized(int w, int h);

		void touchDown(int x, int y, int id);
		void touchMoved(int x, int y, int id);
		void touchUp(int x, int y, int id);
		void touchDoubleTap(int x, int y, int id);
		void touchCancelled(int x, int y, int id);
		void swipe(ofxAndroidSwipeDir swipeDir, int id);

		void pause();
		void stop();
		void resume();
		void reloadTextures();

		bool backPressed();
		void okPressed();
		void cancelPressed();
};
