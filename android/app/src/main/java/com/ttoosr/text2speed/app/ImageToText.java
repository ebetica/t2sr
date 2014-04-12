package com.ttoosr.text2speed.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by zeming on 4/12/14.
 */
public class ImageToText {
    private String TESSBASE_PATH;
    private static final String DEFAULT_LANGUAGE = "eng";

    private TessBaseAPI baseApi;

    public ImageToText(Context context, boolean hardInit) {
        if(hardInit) {
            copyAssets(context);
        }
        TESSBASE_PATH = context.getFilesDir().getPath();
        Log.d("PATH_DEBUG", "Actual Path = " + TESSBASE_PATH);
        baseApi = new TessBaseAPI();
        baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
    }

    private void copyAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("PATH_DEBUG", "Failed to get asset file list.", e);
        }
        String dir = (new File(context.getFilesDir(), "tessdata")).getPath();
        File tessData = new File(dir);
        if (!tessData.mkdirs()) Log.d("PATH_DEBUG", "Failed to create tessdata dir");
        for(String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = assetManager.open(filename);
                File outFile = new File(dir, filename);
                Log.d("Copying", "Copying " + filename + " ... ");
                out = new FileOutputStream(outFile);
                Log.d("Copying", "...to " + outFile.getAbsolutePath());
                copyFile(in, out);
                Log.d("Copying", "Copied! Now we just need to close the streams");
                in.close();
                out.flush();
                out.close();
            } catch(IOException e) {
                Log.e("PATH", "Failed to copy asset file: " + filename, e);
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public String getString(Bitmap bmp) {
        baseApi.setImage(bmp);
        return baseApi.getUTF8Text();
    }
}
