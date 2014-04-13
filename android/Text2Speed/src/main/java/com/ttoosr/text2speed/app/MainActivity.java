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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import pro.dbro.openspritz.MainSpritzActivity;

public class MainActivity extends Activity {
    enum ResumeMode
    {
        MODE_UNINITIALIZED,
        MODE_PICTURE,
        MODE_READER
    }

    private static final int REQUEST_CAPTURE_IMAGE_ACTIVITY = 100;
    private static final int REQUEST_VIEW_OCR_TEXT = 200;
    private static final String APP_NAME = "Text2Speed";
    private static final String BUNDLE_KEY_SOFT_RESET = "SOFT_RESET";
    private static final String BUNDLE_KEY_CAPTURE_URI = "CAPTURE_URI";
    private static final String BUNDLE_KEY_PARSED_TEXT = "PARSED_TEXT";
    private static final String BUNDLE_KEY_MODE = "MODE";
    Uri m_captureUri;
    ImageToText m_im2txt;
    String m_parsedText;
    ResumeMode m_mode = ResumeMode.MODE_UNINITIALIZED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_t2sr_main);
        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(BUNDLE_KEY_SOFT_RESET)) {
                m_im2txt = new ImageToText(this, false);
            } else {
                m_im2txt = new ImageToText(this, true);
            }
            m_captureUri = savedInstanceState.getParcelable(BUNDLE_KEY_CAPTURE_URI);
            m_parsedText = savedInstanceState.getString(BUNDLE_KEY_PARSED_TEXT);
            m_mode = (ResumeMode)savedInstanceState.getSerializable(BUNDLE_KEY_MODE);
        }
        Log.d(APP_NAME,m_mode.toString());
        Log.d(APP_NAME,String.valueOf(m_captureUri));
    }

    /**
     * Handles executing the state transitions set up in onActivityResult.
     */
    @Override
    public void onResume()
    {
        super.onResume();
        if(m_mode == ResumeMode.MODE_UNINITIALIZED) {
            activateOCRImageCamera();
        } else if(m_mode == ResumeMode.MODE_PICTURE) {
            activateOCRImageCamera();
        } else if(m_mode == ResumeMode.MODE_READER) {
            activateOCRTextReader();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putBoolean(BUNDLE_KEY_SOFT_RESET, true);
        outState.putParcelable(BUNDLE_KEY_CAPTURE_URI, m_captureUri);
        outState.putString(BUNDLE_KEY_PARSED_TEXT, m_parsedText);
        outState.putSerializable(BUNDLE_KEY_MODE, m_mode);
        super.onSaveInstanceState(outState);
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile() {
        File mediaStorageDir = getOutputDir();
        if(mediaStorageDir == null) {
            return null;
        }
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_OCR.jpg");
        return mediaFile;
    }
    private static File getOutputDir() {
        // TODO: check if the storage medium is usable
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), APP_NAME);
        if(!dir.exists()) {
            if(!dir.mkdirs()) {
                Log.d(APP_NAME, "Failed to create output directory");
                return null;
            }
        }
        return dir;
    }
    private static File getOutputTextFile() {
        File mediaStorageDir = getOutputDir();
        if(mediaStorageDir == null) {
            return null;
        }
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "TXT_OCR.txt");
        return mediaFile;
    }

    /**
     * Handles the state transitions between activities.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CAPTURE_IMAGE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                if(m_captureUri == null) {
                    Log.d(APP_NAME, "FUCK. Capture URI is null.");
                    return;
                }
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
                String imgStr = m_im2txt.getString(ocrImg);
                Log.d(APP_NAME, "Parsed image:\n" + imgStr);
                Log.d(APP_NAME, "Setting mode to reader.");
                m_parsedText = imgStr;
                m_mode = ResumeMode.MODE_READER;
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
        else if(requestCode == REQUEST_VIEW_OCR_TEXT) {
            if(resultCode == RESULT_OK) {
                Log.d(APP_NAME, "Setting mode to picture.");
                m_mode = ResumeMode.MODE_PICTURE;
            } else if(resultCode == RESULT_CANCELED) {
                Log.d(APP_NAME, "Canceled");
            } else {
                Log.d(APP_NAME, "other");
            }
        }
    }

    public void activateOCRTextReader()
    {
        Log.d(APP_NAME,"Activating reader mode.");
        String text = m_parsedText;
        if(m_parsedText == null) {
            Log.d(APP_NAME, "FUCK. Parsed OCR text is null.");
            return;
        }
        File txtFile = getOutputTextFile();
        try {
            PrintWriter ofWriter = new PrintWriter(new FileOutputStream(txtFile));
            ofWriter.print(text);
            ofWriter.close();
        } catch (FileNotFoundException e) {
            Log.d(APP_NAME,"FUCK SHIT STACK");
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.fromFile(txtFile));
        intent.setClass(this, MainSpritzActivity.class);
        startActivityForResult(intent, REQUEST_VIEW_OCR_TEXT);
    }
    public void activateOCRImageCamera()
    {
        Log.d(APP_NAME,"Activating camera mode.");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri capUri = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capUri);
        m_captureUri = capUri;
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE_ACTIVITY);
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
