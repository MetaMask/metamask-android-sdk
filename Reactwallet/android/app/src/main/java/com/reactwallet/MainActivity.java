package com.reactwallet;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.ReactRootView;

public class MainActivity extends ReactActivity {

  private ActivityResultLauncher<Intent> requestPermissionLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

//    Intent launchAppIntent = getIntent();
//    Log.d("REACT_MAIN_ACTIVITY", "Oncreate");
//
//    boolean openAsOverlay = (launchAppIntent != null && launchAppIntent.getBooleanExtra("open_as_overlay", false));
//    boolean launchedByDapp = "com.metamask.android".equals(launchAppIntent.getPackage());
//    boolean launchedBySdk = "io.metamask.androidsdk".equals(launchAppIntent.getPackage());
//    System.out.println(launchAppIntent.getExtras());
//
//    if (launchedBySdk) {
//      Log.d("REACT_MAIN_ACTIVITY", "Launched by SDK");
//    } else if (launchedByDapp) {
//      Log.d("REACT_MAIN_ACTIVITY", "Launched by Dapp");
//    }
//    if (openAsOverlay) {
//      Log.d("REACT_MAIN_ACTIVITY", "Opening as overlay");
//      if (Settings.canDrawOverlays(this)) {
//        // The app already has permission to draw over other apps
//        Intent intent = new Intent(this, MetaMaskPartialOverlayService.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//          Log.d("REACT_MAIN_ACTIVITY", "Launching as foreground service");
//          startService(intent);
//        } else {
//          Log.d("REACT_MAIN_ACTIVITY", "Launching as normal service");
//          startService(intent);
//        }
//      } else {
//        // The app does not have permission to draw over other apps
//        requestPermissionLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                  // Handle the result of the permission request
//                  if (Settings.canDrawOverlays(this)) {
//                    // The user has granted permission for the app to draw overlays
//                    Log.d("REACT_MAIN_ACTIVITY", "Launching partial overlay");
//                    Intent intent = new Intent(this, MetaMaskPartialOverlayService.class);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                      Log.d("REACT_MAIN_ACTIVITY", "Launching as foreground service");
//                      startForegroundService(intent);
//                    } else {
//                      Log.d("REACT_MAIN_ACTIVITY", "Launching as normal service");
//                      startService(intent);
//                    }
//                  } else {
//                    // The user has denied permission for the app to draw overlays
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                    requestPermissionLauncher.launch(intent);
//                  }
//                });
//
//        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//        requestPermissionLauncher.launch(intent);
//      }
//    } else {
//      Log.d("REACT_MAIN_ACTIVITY", "opening as usual");
//    }
  }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "Reactwallet";
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
