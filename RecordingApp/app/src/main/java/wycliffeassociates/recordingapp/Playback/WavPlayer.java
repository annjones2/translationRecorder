package wycliffeassociates.recordingapp.Playback;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.Vector;

import wycliffeassociates.recordingapp.AudioInfo;
import wycliffeassociates.recordingapp.AudioVisualization.CanvasView;
import wycliffeassociates.recordingapp.Playback.Editing.CutOp;
import wycliffeassociates.recordingapp.Reporting.Logger;

/**
 * Plays .Wav audio files
 */
public class WavPlayer {

    private static MappedByteBuffer audioData = null;
    private static AudioTrack player = null;
    private static Thread playbackThread;
    private static volatile boolean onlyPlayingSection = false;
    private static volatile int endPlaybackPosition = 0;
    private static volatile int startPlaybackPosition = 0;
    private static int minBufferSize = 0;
    private static volatile boolean keepPlaying = false;
    private static volatile int playbackStart = 0;
    private static volatile boolean forceBreakOut = false;
    private static CutOp sCutOp;
    private static volatile boolean sPressedSeek = true;
    private static volatile boolean sPressedPause = false;
    private static volatile Vector<Pair<Integer,Integer>> sSkippedStack;

    public static void setCutOp(CutOp cut){
        sCutOp = cut;
    }

    public static void setOnlyPlayingSection(Boolean onlyPlayingSection){
        WavPlayer.onlyPlayingSection = onlyPlayingSection;
    }

    public static void resetState(){
       onlyPlayingSection = false;
       endPlaybackPosition = 0;
       startPlaybackPosition = 0;
       minBufferSize = 0;
       keepPlaying = false;
       playbackStart = 0;
       forceBreakOut = false;
    }

    public static void skipCutSection(){
        final int skipSection = sCutOp.skip((int)(audioData.position()/88.2));
        if(skipSection != -1) {
            player.pause();
            player.flush();
            //500 instead of 1000 because the position should be double here- there's two bytes
            //per data point in the audio array
            playbackStart = (int) (skipSection * (AudioInfo.SAMPLERATE / 500.0));
            //make sure the playback start is within the bounds of the file's capacity
            playbackStart = Math.max(Math.min(audioData.capacity(), playbackStart), 0);
            int position = (playbackStart % 2 == 0) ? playbackStart : playbackStart + 1;
            System.out.println("starting from position " + playbackStart);
            //position in the buffer keeps track of where we are for playback
            audioData.position(position);
            player.play();
        }
    }

    public static void play(){
        forceBreakOut = false;
        if(WavPlayer.isPlaying()){
            return;
        }
        keepPlaying = true;
        player.flush();
        player.play();
        if(!onlyPlayingSection && !sPressedSeek && !sPressedPause){
            playbackStart = 0;
        }
        sPressedPause = false;
        sPressedSeek = false;
        playbackThread = new Thread(){

            public void run(){
                //the starting position needs to beginning of the 16bit PCM data, not in the middle
                int position = (playbackStart % 2 == 0)? playbackStart : playbackStart+1;
                System.out.println("starting from position " + playbackStart);
                //position in the buffer keeps track of where we are for playback
                audioData.position(position);
                int limit = audioData.capacity();
                short[] shorts = new short[minBufferSize/2];
                byte[] bytes = new byte[minBufferSize];
                while(audioData.position() < limit && keepPlaying){

                    //checks to see if we're in a selected section and should end
                    if(checkIfShouldStop()){
                        break;
                    }
//                        int numSamplesLeft = limit - audioData.position();
//                        //if the number of samples left is large enough, just copy the data
//                        if(numSamplesLeft >= bytes.length) {
//                            //need to grab data from the mapped file, then convert it into a short array
//                            //since AudioTrack requires writing shorts for playing PCM16
//                            audioData.get(bytes);
//                        }
//                        //if the number of samples left wont fill the buffer, zero out the end
//                        else {
//                            for(int i=numSamplesLeft; i<shorts.length; i++) {
//                                shorts[i] = 0;
//                            }
//                            for(int i=numSamplesLeft; i<bytes.length; i++) {
//                                bytes[i] = 0;
//                            }
//                            audioData.get(bytes, 0, numSamplesLeft);
//                        }
                    get(bytes);
                    //copy the bytes from the audio file into a short buffer, need to flip byte order
                    //as wav files are little endian
                    ByteBuffer bytesBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                    bytesBuffer.asShortBuffer().get(shorts);
                    //write the buffer to the audiotrack; the write is blocking
                    player.write(shorts, 0, shorts.length);
                }
                //location doesn't usually end up going to the end before audio playback stops.
                //continue to loop until the end is reached.
                while((getLocation() <= (getDuration())) && !forceBreakOut){
                    Thread.yield();
                }
                System.out.println("end thread");
                System.out.println("location is " + getLocation() + " out of " + getDuration());
            }
        };
        playbackThread.start();
    }

    private static void get(byte[] bytes){
        int size = bytes.length;
        int skip = 0;
        int end = 0;
        boolean brokeEarly = false;
        for(int i = 0; i < size; i++){
            if(!audioData.hasRemaining()){
                brokeEarly = true;
                end = i;
                break;
            }
            skip = sCutOp.skip((int)(audioData.position()/88.2));
            if(skip != -1 && i % 2 == 0){
                Logger.i(WavPlayer.class.toString(), "Location is " + getLocation() + "position is " + audioData.position());
                int start = (int) (skip * (AudioInfo.SAMPLERATE / 500.0));
                //make sure the playback start is within the bounds of the file's capacity
                start = Math.max(Math.min(audioData.capacity(), start), 0);
                int position = (start % 2 == 0) ? start : start + 1;
                audioData.position(position);
                Logger.i(WavPlayer.class.toString(), "Location is now " + getLocation()  + "position is " + audioData.position());
            }
            bytes[i] = audioData.get();
        }
        if(brokeEarly){
            for(int i = end; i < size; i++){
                bytes[i] = 0;
            }
        }
    }

    /**
     * Sets the audio data to play back; this expects a mapped buffer of PCM data
     * Header of .wav files should not be included in this mapped buffer
     * Initializes the audio track to play this file
     * @param file
     */
    public static void loadFile(MappedByteBuffer file){
        resetState();
        audioData = file;
        minBufferSize = AudioTrack.getMinBufferSize(AudioInfo.SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        System.out.println("buffer size for playback is " + minBufferSize);
        System.out.println("length of audio data buffer is " +audioData.capacity());

        player = new AudioTrack(AudioManager.STREAM_MUSIC, AudioInfo.SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM);
    }

    //Pause calls flush so as to eliminate data that may have been written right after the pause
    public static void pause(boolean fromButtonPress){
        if(player != null){
            playbackStart = (int)(getLocation() * 88.2);
            sPressedPause = true;
            pause();
        }
    }

    public static void pause(){
        if(player != null){
            player.pause();
            player.flush();
            forceBreakOut = true;
            keepPlaying = false;
        }
    }

    public static boolean exists(){
        if(player != null){
            return true;
        } else
            return false;
    }

    public static void seekToStart(){
        if(player != null ) {
            if(onlyPlayingSection){
                seekTo(startPlaybackPosition);
            }
            else {
                seekTo(0);
            }
        }
    }

    public static void seekToEnd(){
        if(player != null ) {
            if(onlyPlayingSection){
                seekTo(endPlaybackPosition);
            }
            else {
                seekTo(sCutOp.timeAdjusted(WavPlayer.getDuration() - sCutOp.getSizeCut()));
            }
        }
    }

    public static void seekTo(int x){
        boolean wasPlaying = isPlaying();
        sPressedSeek = true;
        stop();
//        int seekTo = sCutOp.skip(x);
//        if(seekTo != -1){
//            x = seekTo;
//        }
        //500 instead of 1000 because the position should be double here- there's two bytes
        //per data point in the audio array
        playbackStart = (int)(x * (AudioInfo.SAMPLERATE/500.0));
        //make sure the playback start is within the bounds of the file's capacity
        playbackStart = Math.max(Math.min(audioData.capacity(), playbackStart), 0);
        if(wasPlaying){
            play();
        }
    }

    public static void stop(){
        if(isPlaying() || isPaused()){
            keepPlaying = false;
            player.pause();
            player.stop();
            if(playbackThread != null){
                forceBreakOut = true;
                playbackThread = null;
            }
            player.flush();
        }
    }

    public static void stopSectionAt(int end){
        endPlaybackPosition = end;
        onlyPlayingSection = true;
    }

    public static void startSectionAt(int startMS){
        startPlaybackPosition = startMS;
    }

    public static boolean checkIfShouldStop(){
        if((WavPlayer.getDuration()) <= getLocation()) {
            pause();
            return true;
        }
        if(onlyPlayingSection && (getLocation() >= endPlaybackPosition)){
            pause();
            seekTo(startPlaybackPosition);
            stop();
            return true;
        }
        return false;
    }

    public static void release(){
        stop();
        audioData = null;
        if(player != null)
            player.release();
        player = null;
        if(playbackThread!= null){
            keepPlaying = false;
            forceBreakOut = true;
        }
    }

    public static boolean isPlaying(){
        if(player != null)
            return player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
        else
            return false;
    }

    public static boolean isPaused(){
        if(player != null)
            return player.getPlayState() == AudioTrack.PLAYSTATE_PAUSED;
        else
            return false;
    }

    public static int getLocation(){
        if(player != null) {
            int loc = Math.min((int) ((playbackStart / 2 + player.getPlaybackHeadPosition()) *
                    (1000.0 / AudioInfo.SAMPLERATE)), getDuration());
//            if(mMovedBackwards){
//                loc = sCutOp.reverseTimeAdjusted(loc, (int) (playbackStart / 88.2));
//            } else {
            //Ignore cuts prior to playback start: assume they're already accounted for
            loc = sCutOp.timeAdjusted(loc, (int) (playbackStart / 88.2));
           // }
            return loc;
        }
        else {
            forceBreakOut = true;
            return 0;
        }
    }

    public static int getAdjustedLocation(){
        if(player != null) {
            int loc = sCutOp.reverseTimeAdjusted(getLocation());
            return loc;
        }
        else {
            return 0;
        }
    }

    public static int getDuration(){
        if(player != null){
            int duration = (int)(audioData.capacity()/((AudioInfo.SAMPLERATE/1000.0) * AudioInfo.BLOCKSIZE));
            return duration;
        }
        else {
            return 0;
        }
    }

    public static int getAdjustedDuration(){
        return getDuration() - sCutOp.getSizeCut();
    }

    public static int getSelectionEnd(){
        return endPlaybackPosition;
    }

}