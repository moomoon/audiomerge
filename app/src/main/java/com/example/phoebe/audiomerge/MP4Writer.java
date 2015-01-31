package com.example.phoebe.audiomerge;

/**
 * Created by Phoebe on 1/31/15.
 */

import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;
import com.googlecode.mp4parser.authoring.tracks.SilenceTrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by leapin on 2015/1/14.
 */
public class MP4Writer {
    private List<H264FileDescriptor> mH264Files;
    private List<AACFileDescriptor> mAACFiles;
    private List<ExtractedAudioDescriptor> mExtractedAudios;

    public MP4Writer() {
        mH264Files = new ArrayList<>();
        mAACFiles = new ArrayList<>();
        mExtractedAudios = new ArrayList<>();
    }

    public void addH264File(H264FileDescriptor fd) {
        mH264Files.add(fd);
    }

    public void addAACFile(AACFileDescriptor fd) {
        mAACFiles.add(fd);
    }

    public void addExtractedAudio(ExtractedAudioDescriptor fd) {
        mExtractedAudios.add(fd);
    }


    public void writeToMovieFile(String filePath) throws IOException {
        Movie movie = new Movie();
        for (H264FileDescriptor h264file : mH264Files) {
            H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(h264file.filePath), h264file.lang, h264file.timeScale, h264file.frameTicks);
            movie.addTrack(h264Track);
        }
        for (AACFileDescriptor aacFile : mAACFiles) {
            AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacFile.filePath), aacFile.lang);
            movie.addTrack(aacTrack);
        }
        for (ExtractedAudioDescriptor extractedAudio : mExtractedAudios) {
            Movie extr = MovieCreator.build(extractedAudio.filePath);
            for (Track t : extr.getTracks()) {
                if (t.getHandler().endsWith("soun")) {
                    if (extractedAudio.delayMS > 0) {
                        Track silence = new SilenceTrackImpl(t, extractedAudio.delayMS);
                        movie.addTrack(new AppendTrack(silence, t));
                    } else {
                        movie.addTrack(t);
                    }
                    break;
                }
            }
        }


//        AACTrackImpl aacTrackOriginal = null;
//        try {
//            aacTrackOriginal = new AACTrackImpl(new FileDataSourceImpl("audio.aac"));
//            CroppedTrack aacTrackShort = new CroppedTrack(aacTrackOriginal, 1, aacTrackOriginal.getSamples().size());
//            movie.addTrack(aacTrackShort);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
// removes the first sample and shortens the AAC track by ~22ms

        Container mp4file = new DefaultMp4Builder().build(movie);
        FileChannel fc = new FileOutputStream(new File(filePath)).getChannel();
        mp4file.writeContainer(fc);
        fc.close();
    }

    public static class H264FileDescriptor {
        private int timeScale;
        private int frameTicks;
        private String lang;
        private String filePath;

        public H264FileDescriptor(String filePath, String lang, int timeScale, int frameTicks) {
            this.filePath = filePath;
            this.lang = lang;
            this.timeScale = timeScale;
            this.frameTicks = frameTicks;
        }

        public H264FileDescriptor(String filePath) {
            this(filePath, "eng", -1, -1);
        }
    }

    public static class AACFileDescriptor {
        private String lang;
        private String filePath;

        public AACFileDescriptor(String filePath, String lang) {
            this.filePath = filePath;
            this.lang = lang;
        }

        public AACFileDescriptor(String filePath) {
            this(filePath, "eng");
        }
    }

    public static class ExtractedAudioDescriptor {
        private String lang;
        private String filePath;
        private int delayMS;

        public ExtractedAudioDescriptor(String filePath, String lang, int delayMS) {
            this.filePath = filePath;
            this.lang = lang;
            this.delayMS = delayMS;
        }

        public ExtractedAudioDescriptor(String filePath, int delayMS) {
            this(filePath, "eng", delayMS);
        }
    }

//
//    public void write() throws IOException {
//
//        H264TrackImpl h264Track = null;
//        h264Track = new H264TrackImpl(new FileDataSourceImpl("video.h264"));
//        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl("audio.aac"));
////        These Track object are then added to a Movie object
//        Movie movie = new Movie();
//        movie.addTrack(h264Track);
//        movie.addTrack(aacTrack);
////        The Movie object is fed into an MP4Builder to create the container.
//        Container mp4file = new DefaultMp4Builder().build(movie);
////        Write the container to an appropriate sink.
//        FileChannel fc = new FileOutputStream(new File("output.mp4")).getChannel();
//        mp4file.writeContainer(fc);
//        fc.close();
//
//    }
}
