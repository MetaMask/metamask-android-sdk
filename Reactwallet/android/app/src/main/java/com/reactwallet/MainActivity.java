package com.reactwallet;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.ReactApplicationContext;

public class MainActivity extends ReactActivity {

  private ActivityResultLauncher<Intent> requestPermissionLauncher;

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "Reactwallet";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Settings.canDrawOverlays(this)) {
      // The app already has permission to draw over other apps
      Log.d("MM_MOBILE", "Can draw overlays");
      Intent intent = new Intent(this, OverlayService.class);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Log.d("MM_MOBILE", "Starting foreground service");
        startForegroundService(intent);
      } else {
        Log.d("MM_MOBILE", "Starting intent service");
        startService(intent);
      }
    } else {
      Log.d("MM_MOBILE","Requesting permission");
      // The app does not have permission to draw over other apps
      requestPermissionLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              result -> {
                // Handle the result of the permission request
                if (Settings.canDrawOverlays(this)) {
                  Log.d("MM_MOBILE","Granted permission!");
                  // The user has granted permission for the app to draw overlays
                  Intent intent = new Intent(this, OverlayService.class);
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                  } else {
                    Log.d("MM_MOBILE","Starting request permission");
                    startService(intent);
                  }//MM_MOBILE
                } else {
                  Log.d("MM_MOBILE","Not granted permission");
                  // The user has denied permission for the app to draw overlays
                  Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                  requestPermissionLauncher.launch(intent);
                }
              });

      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
      requestPermissionLauncher.launch(intent);
    }
  }

  /**
   * Returns the instance of the {@link ReactActivityDelegate}. There the RootView is created and
   * you can specify the rendered you wish to use (Fabric or the older renderer).
   */
  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new MainActivityDelegate(this, getMainComponentName());
  }

  public static class MainActivityDelegate extends ReactActivityDelegate {
    public MainActivityDelegate(ReactActivity activity, String mainComponentName) {
      super(activity, mainComponentName);
    }

    @Override
    protected ReactRootView createRootView() {
      ReactRootView reactRootView = new ReactRootView(getContext());
      // If you opted-in for the New Architecture, we enable the Fabric Renderer.
      reactRootView.setIsFabric(BuildConfig.IS_NEW_ARCHITECTURE_ENABLED);
      return reactRootView;
    }
  }
}
