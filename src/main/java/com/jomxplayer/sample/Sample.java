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
        player
                .setMute(true)
                .setAspectMode(OmxplayerProcess.AspectMode.LETTERBOX)
                .setWindow(x1, y1, x2, y2);


        player.play();
    }

}
