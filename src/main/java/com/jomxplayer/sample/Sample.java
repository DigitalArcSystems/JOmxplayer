package com.jomxplayer.sample;

import com.jomxplayer.core.OmxplayerProcess;

import java.io.IOException;

/**
 * A Sample class used to show OmxplayerProcess capabilities
 */
public class Sample {

    public static void main(String[] args) throws IOException {

        String videoPath = args[0];

        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;

        OmxplayerProcess player = new OmxplayerProcess(videoPath);
        System.out.println("Video Resolution: "+player.getNativeWidth()+"x"+player.getNativeHeight());

        x1 = 1280/2 - player.getNativeWidth()/2;
        y1 = 800/2 - player.getNativeHeight()/2;
        x2 = player.getNativeWidth()+x1;
        y2 = player.getNativeHeight()+y1;

        final boolean finished[] = {false};
        player
                //.setMute(true)
                //.setAspectMode(OmxplayerProcess.AspectMode.LETTERBOX)
                //.setWindow(x1, y1, x2, y2)
                .setOnEndOfMedia(() -> {
                    finished[0] = true;
                });

        for (String nextFile : args) {
            finished[0] = false;
            System.out.println("Starting "+nextFile);
            player.play(nextFile);
            long start_time_ms = System.currentTimeMillis();
            while (!finished[0]) {
                try {
                    Thread.sleep(100);
                    if (System.currentTimeMillis() - start_time_ms > 5000) {
                        player.stop();
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
