package com.example.phoebe.audiomerge;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.ffmpeg.android.Clip;
import org.ffmpeg.android.FfmpegController;
import org.ffmpeg.android.ShellUtils;

import java.io.File;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AudioMerger am = new AudioMerger("merger");
        am.addSource(Environment.getExternalStorageDirectory() + "/kaipai/vfx/pocoyo/sound.m4a",0, false);
        am.addSource(Environment.getExternalStorageDirectory() +"/temp.aac", 0, true);
 //       am.addSource(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "vfx" + File.separator + "ufo" + File.separator + "sound.aac",0.4F,0);
 //       am.addSource(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "camera" + File.separator + "temp.3gp",0.5F, 0);
 //       am.addSource(Environment.getExternalStorageDirectory() + File.separator + "big_buck_bunny.mp4", 0);
        am.prepare(10000);
        am.merge();

  //      MP4Writer writer = new MP4Writer();
 //       writer.addH264File(new MP4Writer.H264FileDescriptor(Environment.getExternalStorageDirectory() + File.separator + "test.h264", "eng", 30, 1));

//        writer.addExtractedAudio(new MP4Writer.ExtractedAudioDescriptor(Environment.getExternalStorageDirectory() + File.separator + "kaipai" + File.separator + "camera" + File.separator + "temp.3gp", 0));
 //       writer.addExtractedAudio(new MP4Writer.ExtractedAudioDescriptor(Environment.getExternalStorageDirectory() + "/kaipai/vfx/dragon/sound.m4a", 0));
 //       writer.addAACFile(new MP4Writer.AACFileDescriptor(Environment.getExternalStorageDirectory()  + "/kaipai/vfx/ufo/sound.aac"));
//        try {
//            writer.writeToMovieFile(Environment.getExternalStorageDirectory()+ File.separator + "soundmerge.mp4");
 //       } catch (IOException e) {
 //           e.printStackTrace();
 //       }

        try {
//            new CrossfadeTest().test(Environment.getExternalStorageDirectory() + "/kaipai/vfx/ufo",Environment.getExternalStorageDirectory() +"/kaipai/camera/temp.3gp",
//                    Environment.getExternalStorageDirectory() + "/crossfade", 3.4D);
//            new MixTest().test(Environment.getExternalStorageDirectory()+ "/temp",
//                    Environment.getExternalStorageDirectory() + "/kaipai/vfx/ufo/ufo.mp4",
//                    Environment.getExternalStorageDirectory() + "/kaipai/vfx/ufo/sound.aac",
//                    new Clip(Environment.getExternalStorageDirectory() + "/ffmpegMix.mp4")
 //                   );



            Log.e("audioMerger","start");
            FfmpegController fc = new FfmpegController(this, new File(Environment.getExternalStorageDirectory() + "/temp"));
            fc.checkFilters(new ShellUtils.ShellCallback() {
                @Override
                public void shellOut(String shellLine) {
                    Log.e("audioMerger", "shell < " + shellLine);

                }

                @Override
                public void processComplete(int exitValue) {

                }
            });
            fc.mergeAudio(new Clip(Environment.getExternalStorageDirectory() + "/kaipai/camera/temp.3gp"), new Clip(Environment.getExternalStorageDirectory() + "/kaipai/vfx/pocoyo/sound.m4a"), new Clip(Environment.getExternalStorageDirectory() + "/mergedpocoyo.aac"), 3000, 7000, new ShellUtils.ShellCallback() {
                @Override
                public void shellOut(String shellLine) {
                    Log.e("audioMerger", "shell < " + shellLine);

                }

                @Override
                public void processComplete(int exitValue) {

                }
            });

            Log.e("audioMerger", "end");
           // ConcatTest.test(this, Environment.getExternalStorageDirectory() + "/kaipai/vfx/ufo/", Environment.getExternalStorageDirectory() + "/tmp", Environment.getExternalStorageDirectory() + "/kaipai/vfx/ufo/new.mp4", 1D);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);

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
