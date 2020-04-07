
package com.legit;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import android.content.ContentResolver.openInputStream;
import android.content.Context.getContentResolver;

import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RNLegitStoryShareModule extends ReactContextBaseJavaModule {
  private static final String FILE= "file";
  private static final String BASE64 = "base64";
  private static final String CONTENT = "content";
  private static final String INTERNAL_DIR_NAME = "rnlegitstoryshare";

  private static final String SUCCESS = "success";
  private static final String UNKNOWN_ERROR = "An unknown error occured in RNLegitStoryShare";
  private static final String ERROR_TYPE_NOT_SUPPORTED = "Type not supported by RNLegitStoryShare";
  private static final String ERROR_NO_PERMISSIONS = "Permissions Missing";
  private static final String TYPE_ERROR = "Type Error";
  private static final String MEDIA_TYPE_IMAGE = "image/*";

  private static final String instagramScheme = "com.instagram.android";

  private final ReactApplicationContext reactContext;

  public RNLegitStoryShareModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNLegitStoryShare";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("BASE64", BASE64);
    constants.put("FILE", FILE);
    constants.put("SUCCESS", SUCCESS);
    constants.put("UNKNOWN_ERROR", UNKNOWN_ERROR);
    constants.put("TYPE_ERROR", TYPE_ERROR);
    return constants;
  }

  private String generateFileName(){
    Random r = new Random();
    int hash = r.nextInt(999999);

    return "image-" + hash + ".png";
  }

  private String getFilePath() {
    String externalDir = this.getReactApplicationContext().getExternalCacheDir() + "/";
    String namespaceDir = externalDir + INTERNAL_DIR_NAME + "/";
    String fileName = generateFileName();

    File folder = new File(namespaceDir);

    if (!folder.exists()) {
      Boolean isCreated = folder.mkdir();

      if(!isCreated){
        return externalDir + fileName;
      }
    }

    return namespaceDir + fileName;
  }

  private static File createFile(final String path){
    final File file = new File(path);

    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    return file;
  }

  private static File getSavedImageFileForBase64(final String path, final String data) {
    final byte[] imgBytesData = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
    final File file = createFile(path);
    final FileOutputStream fileOutputStream;

    try {
      fileOutputStream = new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    }

    final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
    try {
      bufferedOutputStream.write(imgBytesData);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        bufferedOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return file;
  }

  private File getFileFromBase64String(String base64ImageData){
    String backgroundAssetPath = getFilePath();
    String data = base64ImageData.substring(base64ImageData.indexOf(",") + 1);

    return getSavedImageFileForBase64(backgroundAssetPath, data);
  }

  private static void copyFile(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    try {
      OutputStream out = new FileOutputStream(dst);
      try {
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  private void _shareToInstagram(@Nullable File backgroundFile, @Nullable File stickerFile, @Nullable String attributionLink, @Nullable String backgroundBottomColor, @Nullable String backgroundTopColor, Promise promise){
    try {
      Intent intent = new Intent("com.instagram.share.ADD_TO_STORY");
      String providerName = this.getReactApplicationContext().getPackageName() + ".fileprovider";
      Activity activity = getCurrentActivity();

      if(backgroundFile != null){
        Uri backgroundImageUri = FileProvider.getUriForFile(activity, providerName, backgroundFile);

        intent.setDataAndType(backgroundImageUri, MEDIA_TYPE_IMAGE);
      } else {
        intent.setType(MEDIA_TYPE_IMAGE);
      }
      if(stickerFile != null){
        Uri stickerAssetUri = FileProvider.getUriForFile(activity, providerName, stickerFile);

        intent.putExtra("interactive_asset_uri", stickerAssetUri );
        activity.grantUriPermission(
                "com.instagram.android", stickerAssetUri , Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }

      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


      if(backgroundBottomColor != null){
        intent.putExtra("bottom_background_color", backgroundBottomColor);
      }

      if(backgroundTopColor != null){
        intent.putExtra("top_background_color", backgroundTopColor);
      }

      if(attributionLink != null){
        intent.putExtra("content_url", attributionLink);
      }

      if (activity.getPackageManager().resolveActivity(intent, 0) != null) {
        activity.startActivityForResult(intent, 0);
        promise.resolve(SUCCESS);
      }else{
        throw new Exception("Couldn't open intent");
      }
    }catch (Exception e){
      promise.reject(UNKNOWN_ERROR, e);
    }
  }

  @ReactMethod
  public void shareToInstagram(ReadableMap config, Promise promise){
    try{
      String backgroundAsset = config.hasKey("backgroundAsset") ? config.getString("backgroundAsset") : null;
      String backgroundBottomColor = config.hasKey("backgroundBottomColor") ? config.getString("backgroundBottomColor") : null;
      String backgroundTopColor = config.hasKey("backgroundTopColor") ? config.getString("backgroundTopColor") : null;
      String stickerAsset = config.hasKey("stickerAsset") ? config.getString("stickerAsset") : null;
      String attributionLink = config.hasKey("attributionLink") ? config.getString("attributionLink") : null;
      String type = config.hasKey("type") ? config.getString("type") : FILE;

      if(backgroundAsset == null && stickerAsset == null){
        Error e = new Error("backgroundAsset and stickerAsset are not allowed to both be null.");
        promise.reject("Error in RNStory Share: No asset paths provided", e);
      }

      File backgroundFile = null;
      File stickerFile = null;

      switch(type){
        case BASE64: {
          if(backgroundAsset != null){
            backgroundFile = getFileFromBase64String(backgroundAsset);

            if(backgroundFile == null){
              throw new Error("Could not create file from Base64 in RNLegitStoryShare");
            }
          }

          if(stickerAsset != null){
            stickerFile = getFileFromBase64String(stickerAsset);

            if(stickerFile == null){
              throw new Error("Could not create file from Base64 in RNLegitStoryShare");
            }
          }
          break;
        }

        case FILE: {

          
          String providerName = this.getReactApplicationContext().getPackageName() + ".fileprovider";

          if(backgroundAsset != null){
            backgroundFile = new File(backgroundAsset);

            if(backgroundFile == null){
              throw new Error("Could not create file in RNLegitStoryShare");
            }
          }

          if(stickerAsset != null){
            stickerFile =new File(stickerAsset);

            if(stickerFile == null){
              throw new Error("Could not create file in RNLegitStoryShare");
            }
          }
          // Instantiate implicit intent with ADD_TO_STORY action,
          // background asset, and attribution link



          // Intent intent = new Intent("com.instagram.share.ADD_TO_STORY");
          // intent.putExtra("interactive_asset_uri", dataSticker);
          // intent.setDataAndType(data, "image/*");

          // intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

          // // Instantiate activity and verify it will resolve implicit intent

          // Activity activity = getCurrentActivity();
          // activity.grantUriPermission(
          //   "com.instagram.android", dataSticker, Intent.FLAG_GRANT_READ_URI_PERMISSION);
          // if (activity.getPackageManager().resolveActivity(intent, 0) != null) {
          //   activity.startActivityForResult(intent, 0);
          // }
          break;
        }
        case CONTENT:{
          backgroundFile = getContentResolver().openInputStream(backgroundAsset);

          if(stickerAsset != null){
            stickerFile =new File(stickerAsset);

            if(stickerFile == null){
              throw new Error("Could not create file in RNLegitStoryShare");
            }
          }
          
          break;
        }

        default: {
          throw new Error(ERROR_TYPE_NOT_SUPPORTED);
        }
      }

      _shareToInstagram(backgroundFile, stickerFile, attributionLink, backgroundBottomColor, backgroundTopColor, promise);
    } catch (NullPointerException e){
      promise.reject(e.getMessage(), e);
    } catch (Exception e){
      promise.reject(UNKNOWN_ERROR, e);
    } catch(Error e) {
      promise.reject(e.getMessage(), e);
    }
  }

  private void canOpenUrl(String packageScheme, Promise promise){
    try{
      Activity activity = getCurrentActivity();
      activity.getPackageManager().getPackageInfo(packageScheme, PackageManager.GET_ACTIVITIES);
      promise.resolve(true);
    } catch (PackageManager.NameNotFoundException e) {
      promise.resolve(false);
    } catch (Exception e) {
      promise.reject(new JSApplicationIllegalArgumentException(
              "Could not check if URL '" + packageScheme + "' can be opened: " + e.getMessage()));
    }
  }

  @ReactMethod
  public void isInstagramAvailable(Promise promise){
    canOpenUrl(instagramScheme, promise);
  }
}
