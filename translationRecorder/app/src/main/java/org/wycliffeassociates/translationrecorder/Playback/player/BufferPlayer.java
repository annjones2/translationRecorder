package org.wycliffeassociates.translationrecorder.Playback.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.door43.tools.reporting.Logger;

import org.wycliffeassociates.translationrecorder.AudioInfo;

/**
 * Plays .Wav audio files
 */
class BufferPlayer {

    private final BufferProvider mBufferProvider;
    private static AudioTrack sPlayer = null;
    private static Thread sPlaybackThread;
    private int minBufferSize = 0;
    private BufferPlayer.OnCompleteListener mOnCompleteListener;
    private short[] mAudioShorts;


    interface OnCompleteListener{
        void onComplete();
    }

    interface BufferProvider {
        int onBufferRequested(short[] shorts);
        void onPauseAfterPlayingXSamples(int pausedHeadPosition);
    }

    BufferPlayer(BufferProvider bp) {
        mBufferProvider = bp;
        init();
    }

    BufferPlayer(BufferProvider bp, BufferPlayer.OnCompleteListener onCompleteListener) {
        if(sPlayer != null || sPlaybackThread != null) {
            release();
        }
        mBufferProvider = bp;
        mOnCompleteListener = onCompleteListener;
        init();
    }

    BufferPlayer setOnCompleteListener(BufferPlayer.OnCompleteListener onCompleteListener){
        mOnCompleteListener = onCompleteListener;
        init();
        return this;
    }

    synchronized void play(final int durationToPlay){
        if(isPlaying()){
            return;
        }
        System.out.println("duration to play " + durationToPlay);
        sPlayer.setPlaybackHeadPosition(0);
        sPlayer.flush();
        sPlayer.setNotificationMarkerPosition(durationToPlay);
        sPlayer.play();
        sPlaybackThread = new Thread(){
            public void run(){
                //the starting position needs to beginning of the 16bit PCM data, not in the middle
                //position in the buffer keeps track of where we are for playback
                int shortsRetrieved = 1;
                int shortsWritten = 0;
                while(!sPlaybackThread.isInterrupted() && isPlaying() && shortsRetrieved > 0){
                    shortsRetrieved = mBufferProvider.onBufferRequested(mAudioShorts);
                    shortsWritten = sPlayer.write(mAudioShorts, 0, minBufferSize);
                    switch (shortsWritten) {
                        case AudioTrack.ERROR_INVALID_OPERATION: {
                            Logger.e(this.toString(), "ERROR INVALID OPERATION");
                            break;
                        }
                        case AudioTrack.ERROR_BAD_VALUE: {
                            Logger.e(this.toString(), "ERROR BAD VALUE");
                            break;
                        }
                        case AudioTrack.ERROR: {
                            Logger.e(this.toString(), "ERROR");
                            break;
                        }
                    }
                }
                System.out.println("shorts written " + shortsWritten);
            }
        };
        sPlaybackThread.start();
    }

    void init(){
        //some arbitrarily larger buffer
        minBufferSize = 10 * AudioTrack.getMinBufferSize(AudioInfo.SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        sPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, AudioInfo.SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM);

        mAudioShorts = new short[minBufferSize];
        if(mOnCompleteListener != null) {
            sPlayer.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    finish();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {
                }
            });
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private synchronized void finish(){
        System.out.println("marker reached");
        sPlayer.stop();
        sPlaybackThread.interrupt();
        mOnCompleteListener.onComplete();
    }

    //Simply pausing the audiotrack does not seem to allow the sPlayer to resume.
    synchronized void pause(){
        sPlayer.pause();
        int location = sPlayer.getPlaybackHeadPosition();
        System.out.println("paused at " + location);
        mBufferProvider.onPauseAfterPlayingXSamples(location);
        sPlayer.setPlaybackHeadPosition(0);
        sPlayer.flush();
    }

    boolean exists(){
        if(sPlayer != null){
            return true;
        } else
            return false;
    }

    synchronized void stop(){
        if(isPlaying() || isPaused()){
            sPlayer.pause();
            sPlayer.stop();
            sPlayer.flush();
        }
        if(sPlaybackThread != null){
            sPlaybackThread.interrupt();
        }
    }

    synchronized void release(){
        stop();
        if(sPlayer != null) {
            sPlayer.release();
        }
    }

    boolean isPlaying(){
        if(sPlayer != null)
            return sPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
        else
            return false;
    }

    boolean isPaused(){
        if(sPlayer != null)
            return sPlayer.getPlayState() == AudioTrack.PLAYSTATE_PAUSED;
        else
            return false;
    }

    int getPlaybackHeadPosition(){
        return sPlayer.getPlaybackHeadPosition();
    }
    int getDuration(){
        return 0;
    }
    int getAdjustedDuration(){
        return 0;
    }
    int getAdjustedLocation(){
        return 0;
    }
    void startSectionAt(int i){
    }
    void seekTo(int i){
    }
    void seekToEnd(){
    }
    void seekToStart(){
    }
    boolean checkIfShouldStop(){
        return true;
    }
    void setOnlyPlayingSection(boolean b){
    }
    void stopSectionAt(int i){
    }
}
