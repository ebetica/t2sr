package pro.dbro.openspritz.formats;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by soltanmm on 4/12/14.
 */
public class Text implements SpritzerMedia {
    String m_text;

    public static Text fromUri(Context context, Uri txtUri)
    {
        Text t = new Text();
        File file = new File(txtUri.getPath());
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new FileReader(file));
        } catch (FileNotFoundException e) {
            Log.e("err","err");
        }
        char[] buf = new char[1024];
        int numRead=0;
        try {
            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
            reader.close();
        } catch (IOException e) {

            Log.e("err","err");
        }
        t.m_text = fileData.toString();
        return t;
    }

    @Override
    public String getTitle() {
        return "OCR";
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getChapterTitle(int chapterNumber) {
        return ".";
    }

    @Override
    public String loadChapter(int chapterNumber) {
        if(chapterNumber == 0)
            return m_text;
        else
            return null;
    }

    @Override
    public int countChapters() {
        return 1;
    }
}
