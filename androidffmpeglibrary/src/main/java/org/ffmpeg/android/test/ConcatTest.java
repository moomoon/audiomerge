package org.ffmpeg.android.test;


import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import net.sourceforge.sox.SoxController;

import org.ffmpeg.android.Clip;
import org.ffmpeg.android.FfmpegController;
import org.ffmpeg.android.ShellUtils;

public class ConcatTest {

	public static void test (Context context, String videoRoot, String fileTmpPath, String fileOut, double fadeLen) throws Exception
	{
		File fileTmp = new File(fileTmpPath);
//		File fileAppRoot = new File("");
		File fileVideoRoot = new File(videoRoot);
		
		FfmpegController fc = new FfmpegController(context, fileTmp);
//		 SoxController sxCon = new SoxController(context, fileAppRoot, null);
		
		ArrayList<Clip> listVideos = new ArrayList<Clip>();
		
		String[] fileList = fileVideoRoot.list();
		for (String fileVideo : fileList)
		{
			if (fileVideo.endsWith("mp4"))
			{
				Clip clip = new Clip();
				clip.path = new File(fileVideoRoot,fileVideo).getCanonicalPath();

    			fc.getInfo(clip);

    			clip.duration = clip.duration-fadeLen;
				listVideos.add(clip);
				
    			
			}
		}
		
		Clip clipOut = new Clip ();
		clipOut.path = new File(fileOut).getCanonicalPath();
		
		fc.concatAndTrimFilesMP4Stream(listVideos, clipOut, false, false, new ShellUtils.ShellCallback() {
			
			@Override
			public void shellOut(String shellLine) {

				System.out.println("fc>" + shellLine);
			}
			
			@Override
			public void processComplete(int exitValue) {
			
				if (exitValue < 0)
					System.err.println("concat non-zero exit: " + exitValue);
			}
		});
		
		 
		
	}
}
