/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mattgrasser.augmentedimages.augmentedimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.mattgrasser.augmentedimages.common.helpers.CameraPermissionHelper;
import com.mattgrasser.augmentedimages.common.helpers.DisplayRotationHelper;
import com.mattgrasser.augmentedimages.common.helpers.FullScreenHelper;
import com.mattgrasser.augmentedimages.common.helpers.SnackbarHelper;
import com.mattgrasser.augmentedimages.common.helpers.TrackingStateHelper;
import com.mattgrasser.augmentedimages.common.rendering.BackgroundRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.msgrasser.augmentedimages.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This app extends the HelloAR Java app to include image tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * AugmentedImage.getTrackingMethod() and render only when the tracking method equals to
 * FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/java/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity {
  private static final String TAG = AugmentedImageActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private ArFragment arFragment;
  private ArSceneView arSceneView;

  private ImageView fitToScanView;
  private RequestManager glideRequestManager;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;

  private boolean shouldConfigureSession = false;

  // Augmented image configuration and rendering.
  // Load a single image (true) or a pre-generated image database (false).
  private final boolean useSingleImage = false;
  // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
  // the
  // database.
  private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();
  private final ArrayList<AugmentedImage> augmentedImagesActive = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // hiding the plane discovery
    arFragment.getPlaneDiscoveryController().hide();
    arFragment.getPlaneDiscoveryController().setInstructionView(null);
    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

    arSceneView= arFragment.getArSceneView();

    // Set up frame for scanner
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    glideRequestManager = Glide.with(this);
    glideRequestManager
        .load(Uri.parse("file:///android_asset/fit_to_scan.png"))
        .into(fitToScanView);

    installRequested = false;
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        // Make sure the ARCore APK is installed, else ask the user to install it
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        session = new Session(/* context = */ this);
      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (Exception e) {
        message = "This device does not support AR";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }

      shouldConfigureSession = true;
    }

    if (shouldConfigureSession) {
      configureSession();
      shouldConfigureSession = false;
      arSceneView.setupSession(session);
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }
    displayRotationHelper.onResume();

    fitToScanView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      arSceneView.pause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(
              this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  private void configureSession() {
    Config config = new Config(session);
    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
    config.setFocusMode(Config.FocusMode.AUTO);
    if (!setupAugmentedImageDatabase(config)) {
      messageSnackbarHelper.showError(this, "Could not setup augmented image database");
    }
    session.configure(config);
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private void onUpdateFrame(FrameTime frametime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);

    // Iterate to update augmentedImageMap, remove elements we cannot draw.
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = String.format("Detected Image %d", augmentedImage.getIndex());
          messageSnackbarHelper.showMessage(this, text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          this.runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  fitToScanView.setVisibility(View.GONE);
                }
              });

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            Anchor centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            augmentedImageMap.put(
                augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage.getIndex());
          break;

        default:
          break;
      }
    }

    // Draw all images in augmentedImageMap
    for (Pair<AugmentedImage, Anchor> pair : augmentedImageMap.values()) {
      AugmentedImage augmentedImage = pair.first;
      if (!augmentedImagesActive.contains(augmentedImage)) {
        Anchor centerAnchor = augmentedImageMap.get(augmentedImage.getIndex()).second;
        switch (augmentedImage.getTrackingState()) {
          case TRACKING:
            renderObject(centerAnchor);
            augmentedImagesActive.add(pair.first);
            break;
          default:
            break;
        }
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private void renderObject(Anchor anchor){
    ModelRenderable.builder()
            .setSource(this,  Uri.parse("geb.sfb"))
            .build()
            .thenAccept(renderable -> addNodeToScene(anchor, renderable))
            .exceptionally((throwable -> {
              AlertDialog.Builder builder = new AlertDialog.Builder(this);
              builder.setMessage(throwable.getMessage())
                      .setTitle("Error!");
              AlertDialog dialog = builder.create();
              dialog.show();
              return null;
            }));
  }

  public void addNodeToScene(Anchor anchor, ModelRenderable renderable) {
    AnchorNode anchorNode = new AnchorNode(anchor);
    TransformableNode node = new TransformableNode(this.arFragment.getTransformationSystem());
    node.setLocalPosition(Vector3.add(node.getLocalPosition(), new Vector3(0, 0.1f, 0)));
    node.setRenderable(renderable);
    node.setParent(anchorNode);
    this.arFragment.getArSceneView().getScene().addChild(anchorNode);
    node.select();

    // remove circle
    // https://github.com/google-ar/sceneform-android-sdk/issues/352#issuecomment-427138254
    this.arFragment.getTransformationSystem().getSelectionVisualizer().removeSelectionVisual(node);

    // hide plane detection
    // https://github.com/google-ar/sceneform-android-sdk/issues/403#issuecomment-433850299
    this.arFragment.getArSceneView().getPlaneRenderer().setVisible(false);
  }

  private boolean setupAugmentedImageDatabase(Config config) {
    AugmentedImageDatabase augmentedImageDatabase;

    // There are two ways to configure an AugmentedImageDatabase:
    // 1. Add Bitmap to DB directly
    // 2. Load a pre-built AugmentedImageDatabase
    // Option 2) has
    // * shorter setup time
    // * doesn't require images to be packaged in apk.
    if (useSingleImage) {
      Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
      if (augmentedImageBitmap == null) {
        return false;
      }

      augmentedImageDatabase = new AugmentedImageDatabase(session);
      augmentedImageDatabase.addImage("image_name", augmentedImageBitmap);
      // If the physical size of the image is known, you can instead use:
      //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
      // This will improve the initial detection speed. ARCore will still actively estimate the
      // physical size of the image as it is viewed from multiple viewpoints.
    } else {
      // This is an alternative way to initialize an AugmentedImageDatabase instance,
      // load a pre-existing augmented image database.
      try (InputStream is = getAssets().open("books.imgdb")) {
        augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
      } catch (IOException e) {
        Log.e(TAG, "IO exception loading augmented image database.", e);
        return false;
      }
    }

    config.setAugmentedImageDatabase(augmentedImageDatabase);
    return true;
  }

  private Bitmap loadAugmentedImageBitmap() {
    try (InputStream is = getAssets().open("default.jpg")) {
      return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      Log.e(TAG, "IO exception loading augmented image bitmap.", e);
    }
    return null;
  }
}
