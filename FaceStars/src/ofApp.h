#pragma once

#include "ofMain.h"
#include "ofxAndroid.h"
#include "ofxCv.h"

class ofApp : public ofxAndroidApp{
	
	public:

		int N;
        vector<int> thicknesses;
        ofMesh mesh;

        ofVideoGrabber vid;
        ofxCv::ObjectFinder finder;
        bool showDebugVid;

        float a_x;
        float a_y;
        float zoom;

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
