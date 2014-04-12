package com.ttoosr.text2speed.app;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final int REQUEST_CAPTURE_IMAGE_ACTIVITY = 100;
    private static final String APP_NAME = "Text2Speed";
    Uri m_captureUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri capUri = Uri.fromFile(getOutputMediaFile());;
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capUri);
        m_captureUri = capUri;
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE_ACTIVITY);
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), APP_NAME);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(APP_NAME, "Failed to create directory");
                return null;
            }
        }
        // Create a media file name
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_OCR.jpg");
        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CAPTURE_IMAGE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                String ocrImgPath = m_captureUri.getPath();
                Log.d(APP_NAME,"OCR image saved to : "+ocrImgPath);
                Bitmap ocrImg = BitmapFactory.decodeFile(ocrImgPath);
                if(ocrImg != null)
                    Log.d(APP_NAME, "Successfully loaded OCR image.");
                else {
                    Log.d(APP_NAME, "OCR image could not be loaded.");
                    Toast.makeText(this, "Failed to read image.", Toast.LENGTH_SHORT);
                    return;
                }
                // TODO: pass ocrImg to the OCR code
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
