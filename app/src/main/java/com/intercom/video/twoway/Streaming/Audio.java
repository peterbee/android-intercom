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

    /**
     * how many audio elements are we trying to capture at once
     * 1024 seems to work nicely
     */
    final int AUDIO_CHUNK_SIZE = 1024;

    /**
     * Bytes per audio element = 2 bytes in 16bit format
     */
    final int BYTES_PER_AUDIO_ELEMENT = 2;

    /**
     * The sample rate.  Default is 8000 which is very low for proof of concept
     * This can probably be safely increased to a higher supported value
     */
    private static final int RECORDER_SAMPLERATE = 8000;

    /**
     * How many audio channels, default is mono
     */
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    /**
     * Default ancoding, default is 16 bt pcm
     */
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Used to control the audio capture loop
     */
    boolean capturingAudio;

    /**
     * Audio track player that plays raw pcm data we feed it
     */
    AudioTrack audioTrackPlayer;

    /**
     * Used to synchronize the audio playback thread when playing a chunk of audio
     * so that multiple pices of audio dont play at once
     */
    static Object audioThreadPlaybackLock = new Object();

    /**
     * This is just a buffer that shrinks and grows.
     * stores our built up audio data, this grows as more data is read
     * and shrinks down when audio data is transmitted.
     */
    ByteArrayOutputStream audioDataStorageStream = new ByteArrayOutputStream();

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
     * This is called each time we send a frame to grab all audio data
     * that has built up since the previous frame
     * @return a byte array containing the contents of audioDataStorageStream;
     */
    public byte[] consumeAudioBytes()
    {
        byte[] audioData = audioDataStorageStream.toByteArray();
        audioDataStorageStream = new ByteArrayOutputStream();
        Log.i("Audio", "Consumed bytes for sending: " + audioData.length);
        return audioData;
    }

    /**
     * starts up our audio player to constantly play data we feed it
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
                    // only capture if the mic is on
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
