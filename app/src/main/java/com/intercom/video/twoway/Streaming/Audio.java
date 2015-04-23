package com.intercom.video.twoway.Streaming;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.intercom.video.twoway.MainActivity;

import java.io.ByteArrayOutputStream;

/**
 * @Author Sean Luther
 * This class handles all audio encoding, decoding, and playback
 */
public class Audio
{
    final int AUDIO_CHUNK_SIZE = 1024; // how many audio elements are we trying to capture at once
    final int BYTES_PER_AUDIO_ELEMENT = 2; // 2 bytes in 16bit format
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    boolean capturingAudio;
    AudioTrack audioTrackPlayer;
    static Object audioThreadPlaybackLock = new Object();
    ByteArrayOutputStream audioDataStorageStream = new ByteArrayOutputStream(); // stores our built up audio data, this grows as more data is read and shrinks down when audio data is transmitted

    /**
     * Constructor
     * calls setupAudioPlayer
     */
    public Audio()
    {
        setupAudioPlayer();
    }

    /**
     * this returns all bytes in audioDataStorageStream and clears it
     *
     * @return a byte array containing the contents of audioDataStorageStream;
     */
    public byte[] consumeAudioBytes()
    {
        byte[] audioData = audioDataStorageStream.toByteArray();
        audioDataStorageStream = new ByteArrayOutputStream(); //this may be the cause of Out Of Memory errors during long runs.  Should be changed to a flushing method?
        Log.i("Audio", "Consumed bytes for sending: " + audioData.length);
        return audioData;
    }

    /**
     * sets up our audio player to constantly play data we feed it
     */
    public void setupAudioPlayer()
    {
        audioTrackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,RECORDER_AUDIO_ENCODING, AUDIO_CHUNK_SIZE * BYTES_PER_AUDIO_ELEMENT, AudioTrack.MODE_STREAM);
        audioTrackPlayer.play();
    }

    /**
     * feeds the audio player a chunk of data to play
     * @param audioDataBuffer
     */
    void playAudioChunk(final byte[] audioDataBuffer)
    {
        Thread audioPlaybackThread = new Thread(new Runnable()
        {
            public void run()
            {
                synchronized(audioThreadPlaybackLock)
                {
                    audioTrackPlayer.write(audioDataBuffer, 0, audioDataBuffer.length);
                }
            }});
        audioPlaybackThread.start();
    }

    /**
     * Start capturing audio data from the mic
     */
    public void startAudioCapture()
    {

        Thread audioCaptureThread = new Thread(new Runnable()
        {
            public void run()
            {
                byte audioDataBuffer[] = new byte[AUDIO_CHUNK_SIZE*BYTES_PER_AUDIO_ELEMENT];
                audioDataStorageStream = new ByteArrayOutputStream();
                AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, AUDIO_CHUNK_SIZE * BYTES_PER_AUDIO_ELEMENT);
                capturingAudio=true;
                audioRecorder.startRecording();

                while (capturingAudio)
                {
                    // tempoary fix to turn mic off so we can demonstrate with no feedback
                    if(MainActivity.mic)
                    {
                        try
                        {
                            // gets the voice output from microphone to byte format
                            audioRecorder.read(audioDataBuffer, 0, AUDIO_CHUNK_SIZE * BYTES_PER_AUDIO_ELEMENT);
                            audioDataStorageStream.write(audioDataBuffer);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                    try
                    {
                        Thread.sleep(100);
                    }
                    catch(Exception e) {
                        Log.d("Audio", "General exception during audio capture from mic");
                    }


                }
                audioRecorder.stop();
                audioRecorder.release();
            }
        });

        Log.i("Audio", "About to start audio");
        audioCaptureThread.start();


    }

}
