package com.jomxplayer.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by e on 12/20/18.
 */
public class BashProcess {

    /**
     *

     1           decrease speed
     2           increase speed
     <           rewind
     >           fast forward
     z           show info
     j           previous audio stream
     k           next audio stream
     i           previous chapter
     o           next chapter
     n           previous subtitle stream
     m           next subtitle stream
     s           toggle subtitles
     w           show subtitles
     x           hide subtitles
     d           decrease subtitle delay (- 250 ms)
     f           increase subtitle delay (+ 250 ms)
     q           exit omxplayer
     p / space   pause/resume
     -           decrease volume
     + / =       increase volume
     left arrow  seek -30 seconds
     right arrow seek +30 seconds
     down arrow  seek -600 seconds
     up arrow    seek +600 seconds


     arrow-up:      ^[[A
     arrow-down:    ^[[B
     arrow-right:   ^[[C
     arrow-left:    ^[[D


     try {
     Robot r = new Robot();
     //there are other methods such as positioning mouse and mouseclicks etc.
     r.keyPress(java.awt.event.KeyEvent.VK_UP);
     r.keyRelease(java.awt.event.KeyEvent.VK_UP);
     } catch (AWTException e) {
     //Teleport penguins
     }

     Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();


     */


    //TODO may need to create robot to do this
    public static final String UP_ARROW     = "^[[A";
    public static final String DOWN_ARROW   = "^[[B";
    public static final String RIGHT_ARROW  = "^[[C";
    public static final String LEFT_ARROW   = "^[[D";
    public static final String CTL_C        = "^C";

    protected OutputStreamWriter stdIn;
    protected BufferedReader  stdOut;
    protected BufferedReader  stdError;
    protected boolean idle = true;
    protected static final String PREVIOUS_COMMAND_FINISHED = "BP PREVIOUS COMMAND FINISHED";
    public static final String DEFAULT_SHELL_COMMAND = "/bin/bash";
    protected Process shell = null;
    protected Thread  outputProcessing;
    protected boolean debug = true;
    private boolean already_quitting;
    private int omxplayer_executed = 0;


    public BashProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(DEFAULT_SHELL_COMMAND);
        shell    = pb.start();
        stdIn    = new OutputStreamWriter(shell.getOutputStream());
        stdOut   = new BufferedReader(new InputStreamReader(shell.getInputStream()));
        stdError = new BufferedReader(new InputStreamReader(shell.getErrorStream()));

        outputProcessing = new Thread(() -> {handleShellOutput();});
        outputProcessing.start();
        startCommand("echo READY!");



    }

    public boolean isIdle() {
        return idle;
    }


    protected void handleShellOutput() {
        if (debug) System.out.println("BashProcess.java: Handling Shell Output");
        while (shell.isAlive()) {
            try {
                String currentStdOut = null;
                if (stdOut.ready()) currentStdOut = stdOut.readLine();
                if (currentStdOut != null) {
                    if (currentStdOut.trim().equals(PREVIOUS_COMMAND_FINISHED)) {
                        idle = true;
                        if (quitting()) setQuitting(false);
                        //fire commandFinishedListeners
                    }
                }
                if (debug && currentStdOut != null) System.out.println(currentStdOut);
                String currentErrorOut = null;
                if (stdError.ready()) stdError.readLine();
                if (currentErrorOut != null) {
                    if (currentErrorOut.contains("Subtitle count:")) omxplayer_executed++;
                    if (debug ) System.out.println(currentErrorOut);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.print(".");

        }
        if (debug) System.out.println("BashProcess.java: NO MORE OUTPUT HANDLING");
    }

    public boolean startCommand(String command) throws IOException {
        if (!isIdle()) return false;
        int thisCommand = omxplayer_executed;
        stdIn.flush();
        stdIn.write("echo;echo;"+command+";echo;echo "+PREVIOUS_COMMAND_FINISHED+"\n");
        stdIn.flush();
        idle = false;
        System.out.println("IDLE IS FALSE");
        while (command.contains("omxplayer") && thisCommand < omxplayer_executed) try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void decreaseSpeed() {
        if (!isIdle()) {
            try {
                stdIn.write("1");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void increaseSpeed() {
        if (!isIdle()) {
            try {
                stdIn.write("1");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void rewind() {
        if (!isIdle()) {
            try {
                stdIn.write("<");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void fastForward() {
        if (!isIdle()) {
            try {
                stdIn.write(">");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void toggelPausePlay() {
        if (!isIdle()) {
            try {
                stdIn.write("p");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void quitBackToCLI() {
        if (!isIdle() && !quitting()) {
            setQuitting(true);
            try {
                stdIn.write("q");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected synchronized  boolean quitting() {
        return already_quitting;
    }

    protected synchronized void setQuitting(boolean amIQuitting) {
        already_quitting = amIQuitting;
    }

    public void decreaseVolume() {
        try {
            if (!isIdle()) {
                stdIn.write("-");
                stdIn.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void increaseVolume(){
        if (!isIdle()) {
            try {
                stdIn.write("+");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void back_30_seconds(){
        if (!isIdle()) {
            try {
                stdIn.write(LEFT_ARROW);
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void forward_30_seconds() {
        if (!isIdle()) {
            try {
                stdIn.write(RIGHT_ARROW);
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void back_10_minutes() {
        try {
            if (!isIdle()) {
                stdIn.write(DOWN_ARROW);
                stdIn.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forward_10_minutes() {
        if (!isIdle()) {
            try {
                stdIn.write(UP_ARROW);
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        if (isIdle()) {
            try {
                stdIn.write("exit\n");
                stdIn.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                quitBackToCLI();

                Thread.sleep(500);

                if (!isIdle()) {
                    stdIn.write(CTL_C);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
