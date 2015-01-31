package com.example.phoebe.audiomerge;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AudioMerger am = new AudioMerger("merger");
        am.addSource(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "camera" + File.separator + "temp.3gp", 0);
//        am.addSource(Environment.getExternalStorageDirectory() + File.separator + "big_buck_bunny.mp4", 0);
        am.addSource(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "vfx" + File.separator + "ufo" + File.separator + "sound.aac",0);
        am.prepare(10000);
        am.merge();

        MP4Writer writer = new MP4Writer();
        writer.addH264File(new MP4Writer.H264FileDescriptor(Environment.getExternalStorageDirectory() + File.separator + "test.h264", "eng", 30, 1));

//        writer.addExtractedAudio(new MP4Writer.ExtractedAudioDescriptor(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "camera" + File.separator + "temp.3gp", 0));
        writer.addExtractedAudio(new MP4Writer.ExtractedAudioDescriptor(Environment.getExternalStorageDirectory() + "/kaipai/vfx/dragon/sound.m4a", 0));
        writer.addAACFile(new MP4Writer.AACFileDescriptor(Environment.getExternalStorageDirectory()  + "/kaipai/vfx/ufo/sound.aac"));
        try {
            writer.writeToMovieFile(Environment.getExternalStorageDirectory()+ File.separator + "soundmerge.mp4");
        } catch (IOException e) {
            e.printStackTrace();
        }


}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
