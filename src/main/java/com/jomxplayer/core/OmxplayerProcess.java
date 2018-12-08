package com.jomxplayer.core;

import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Java Process wrapper for easily handling Omxplayer
 */
public class OmxplayerProcess {

    public enum AspectMode {
        LETTERBOX("letterbox"),
        FILL("fill"),
        STRETCH("stretch");

        private String label;

        AspectMode(String label){
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum AudioOutDevice {
        HDMI("hdmi"),
        LOCAL("local"),
        BOTH("both");

        private String label;

        AudioOutDevice(String label) { this.label = label;}

        @Override
        public String toString() {return label;}
    }

    public static int INVALID_VALUE = Integer.MIN_VALUE;

    private String filePath;
    private boolean mute;
    private AspectMode aspectMode;
    private AudioOutDevice audioOutDevice;
    private int[] window;
    private int native_height = INVALID_VALUE;
    private int native_width = INVALID_VALUE;
    private int millibelles = INVALID_VALUE;
    private String mediaInfo = null;
    private boolean loop;
    private Runnable runOnMediaEnd;
    private boolean no_osd = true;

    private Process process;

    /**
     * @param filePath - File path of the video to play
     * @throws IOException if filePath isn't a valid file or can't be opened.
     */
    public OmxplayerProcess(String filePath) throws IOException {
        this.filePath = filePath;
        File file = new File(filePath);
        if (!file.exists()) throw new IOException(filePath+" doesn't exist or can't be found.");

        //Make sure that the omxplayer process is killed when the Java application exits
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                OmxplayerProcess.this.stop();
            }
        });
    }

    /**
     * This provides the x resolution of video media.  If this isn't available it will launch a process to obtain it.
     * @return the x resolution of the video media if any, or INVALID_VALUE if none.
     */
    public int getNativeWidth() {
        if (native_width < 0) {
            obtainMediaInfo();
        }

        return native_width;
    }


    /**
     * This provides the y resolution of video media.  If this isn't available it will launch a process to obtain it.
     * @return the y resolution of the video media if any, or INVALID_VALUE if none.
     */
    public int getNativeHeight() {
        if (native_height < 0) {
            obtainMediaInfo();
        }

        return native_height;
    }

    /**
     * This provides the output from omxplayer -i.  This will launch a process to obtain that info if it hasn't
     * already been done.
     * @return This is the output of omxplayer -i on the media file or null if unavailable.
     */
    public String getMediaInfo() {
        if (mediaInfo == null) {
            obtainMediaInfo();
        }
        return mediaInfo;
    }

    /**
     * This method determines the resolution of video media.  It actually launches a process of omxplayer just to do this
     */
    private void obtainMediaInfo() {
        //run omxplayer and just retrieve dimensions
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "omxplayer -i \""+filePath+"\"");
        Process tempProcess = null;
        InputStream errorStream = null;
        InputStream inputStream = null;
        try{
            tempProcess = pb.start();
            errorStream = tempProcess.getErrorStream();
            inputStream = tempProcess.getInputStream();
            tempProcess.waitFor();
            String output = convertStreamToString(inputStream);
            String error = convertStreamToString(errorStream);
            mediaInfo = error;
            String[] lines = error.split(System.getProperty("line.separator"));
            boolean done = false;
            for (int i=0; i<lines.length && !done; i++ ) {
                String line = lines[i];
                if (line.contains("Stream") && line.contains("Video")) {
                    //remove everything in parentheses and brackets
                    String cs_line[] = line.split(",");
                    for (int j=0; j<cs_line.length; j++) {
                        String inner_line = cs_line[j].trim();
                        inner_line = inner_line.replaceAll("\\(.*\\)", "").trim();
                        inner_line = inner_line.replaceAll("\\[.*\\]", "").trim();
                        if (inner_line != null && inner_line.matches("[0-9]*[x][0-9]*")) {
                            String numbers[] = inner_line.split("x");
                            if (numbers.length == 2) {
                                try {
                                    native_width = Integer.parseInt(numbers[0]);
                                    native_height = Integer.parseInt(numbers[1]);
                                    done = true;
                                    break;
                                } catch (NumberFormatException nfe)
                                {
                                    System.out.println("NumberFormatException: " + nfe.getMessage());
                                }
                            }
                        }
                    }

                }
            }

        }
        catch (IOException e){
            System.out.println(convertStreamToString(errorStream));
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public OmxplayerProcess showStatusInformation(boolean show_status_information_on_overlay) {
        no_osd = !show_status_information_on_overlay;
    }

    /**
     * Set wheter or not to play audio
     * @param mute - true mute the video, false play audio
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setMute(boolean mute) {
        this.mute = mute;
        return this;
    }

    /**
     * This allows the setting of the volume.  0 is loudest. -6000 is softest.
     * @param millibelles double millibels, default 0, range [-6000:0]
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setVolume(int millibelles) {
        if (millibelles > 0) millibelles = 0;
        else if (millibelles < -6000) millibelles = -6000;

        this.millibelles = millibelles;
        return this;
    }

    /**
     * This sets the volume as a percentage.  Forinstance passing in 50 will play the volume at 50% of recorded
     * strength.  Passing in 150 will play at 50% above recorded strength.
     * NOTE: he formula for converting decimal percentage volumes to millibels is
     *  (2000.0 * (Math.log((percentage/100.0)))).  With volume being 1.0 at 100%.  This method doesn't perform this conversion
     *  but instead takes the raw millibelles.
     * @param percentage percentage of recorded strength (0-100 typically)
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setVolumePercentage(int percentage) {
        double volume = percentage/100.0;
        int millibelles = (int) (2000.0 * (Math.log((volume/100.0))));
        setVolume(millibelles);
        return this;
    }

    /**
     * Set's the bounding box of video on the screen
     * @param x1 - Initial x position of the video
     * @param y1 - Initial y position of the video
     * @param x2 - Ending x position of the video
     * @param y2 - Ending y position of the video
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setWindow(int x1, int y1, int x2, int y2) {
        this.window = new int[]{x1,y1,x2,y2};
        return this;
    }

    /**
     * Given a screen resolution of screen_x and screen_y, this method sets the bounding box for the video so that
     * the video appears in the middle of the screen at it's native resolution.  Please note, no attempt is made here
     * to scale the video if it's too large.
     * @param screen_x - The screens x resolution
     * @param screen_y - The screens y resolution
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setWindowInMiddle(int screen_x, int screen_y) {
        obtainMediaInfo();
        int x = (int) ( (screen_x * 0.5) - (getNativeWidth()*0.5)  );
        int y = (int) ( (screen_y * 0.5) - (getNativeHeight()*0.5) );
        return setWindow(x, y, x+getNativeWidth(), y+getNativeHeight());
    }

    /**
     * Set's the aspect mode of the video. Default: stretch if win is specified, letterbox otherwise
     * @param aspectMode
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setAspectMode(AspectMode aspectMode) {
        this.aspectMode = aspectMode;
        return this;
    }

    /**
     * Set's the audio output device of the Video.  Default: whatever is configured for system audio.
     * @param outputDevice
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setAudioOutputDevice(AudioOutDevice outputDevice) {
        AudioOutDevice audioOutputDevice = outputDevice;
        return this;
    }

    /**
     * Set's whether the media file loops or not.  The media file must be seekable for it to be looped.
     * @param loop true if looping is desired.
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    /**
     * This provides an opportunity for clients to run something at the conclusion of the process.  It does not fire
     * at the conclusion of the media if looping is on.
     * @param runAtEnd
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess setOnEndOfMedia(Runnable runAtEnd) {
        runOnMediaEnd = runAtEnd;
        return this;
    }

    /**
     * Create a Java process and start playing the video
     * @return this instance of Omxplayer
     */
    public OmxplayerProcess play() {
        if(process == null){
            String command = "omxplayer";
            if(mute){
                command += " -n -1";
            }
            if(window != null){
                command += String.format(" --win '%d %d %d %d'", window[0], window[1], window[2], window[3]);
            }
            if(aspectMode != null){
                command += " --aspect-mode " + aspectMode;
            }
            if (audioOutDevice != null) {
                command += " --adev  " + audioOutDevice;
            }
            if (millibelles != INVALID_VALUE) {
                command += " --vol " + millibelles;
            }
            if (loop) {
                command += " --loop";
            }
            if (no_osd) {
                command += " --no-osd";
            }

            command = command + " " + "\""+filePath+"\"";

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);

            try{
                process = pb.start();

                new Thread(() -> {
                    try {
                        process.waitFor();
                        if (runOnMediaEnd != null) runOnMediaEnd.run();
                        process = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }).start();


            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        return this;
    }


    /**
     * This is a simple utility to convert a Stream to a String for parsing
     * @param is stream to convert
     * @return the converted string
     */
    private static String convertStreamToString(java.io.InputStream is) {
       String response = null;
        try {
            response = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Stop the Java process associated with the video
     */
    public void stop() {
        if(process != null){
            try{
                process.getOutputStream().write('q');
                process.getOutputStream().flush();
                process = null;
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
