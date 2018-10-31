package com.example.yangzha.actionmusic001;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final int sampleRate = 44100;
    private final double[] freqOfTone = {261/*C4*/, 293/*D4*/, 329/*E4*/, 349/*F4*/, 392/*G4*/, 440/*A4*/, 493/*B4*/, 523/*C5*/,}; // hz

    private boolean isPlaying = false;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    float[] accelerometerValues = new float[3];

    private TextView accelerometerText, noteText;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerText = (TextView) findViewById(R.id.accelerometer);
        noteText = (TextView) findViewById(R.id.note);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onStop() {
        super.onStop();
        // 取消监听
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values;
            String display = "x:" + accelerometerValues[0] + ", y:" + accelerometerValues[1] + ", z:" + accelerometerValues[2];
            accelerometerText.setText(display);
            if (!isPlaying) {
                if(accelerometerValues[2] >= 15){
                    play("C4", 0);
                }
                else if (accelerometerValues[2] <= -15) {
                    play("D4", 1);
                }
                else if (accelerometerValues[0] >= 15) {
                    play("E4", 2);
                }
                else if (accelerometerValues[0] <= -15) {
                    play("F4", 3);
                }
                else if (accelerometerValues[1] >= 15) {
                    play("G4", 4);
                }
                else if (accelerometerValues[1] <= -15) {
                    play("A4", 5);
                }
            }
        }
    }

    private void play(String noteName, int note)
    {
        String id = UUID.randomUUID().toString();
        isPlaying = true;
        noteText.setText(noteName);
        Log.i("playSound", id + "-F4");
        PlayThread thread_playNote = new PlayThread(note, 0.25, id);
        thread_playNote.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class PlayThread extends Thread {
        int note;
        double duration;  //ms
        String id;

        private PlayThread(int note, double duration, String id) {
            this.note = note;
            this.duration = duration;
            this.id = id;
        }

        @Override
        public void run() {
            int numSamples = (int)(duration * sampleRate);
            double sample[] = new double[numSamples];
            byte generatedSnd[] = new byte[2 * numSamples];
            Log.i("GenTone", "Start genTone!");
            genTone(numSamples, sample, generatedSnd);
            Log.i("GenTone", "End genTone");
            playSound(generatedSnd);
        }

        void genTone(int numSamples, double[] sample, byte[] generatedSnd){
            // fill out the array
            for (int i = 0; i < numSamples; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone[this.note]));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (final double dVal : sample) {
                // scale to maximum amplitude
                final short val = (short) ((dVal * 32767));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }
        }

        void playSound(byte[] generatedSnd){
            try {
                Log.i("Write", "Start write!");
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                        AudioTrack.MODE_STATIC);
                audioTrack.write(generatedSnd, 0, generatedSnd.length);
                Log.i("playSound", "isPlaying:" + id + ":" + isPlaying);
                Log.i("Write", "Start write!");
                audioTrack.play();
                try {
                    Thread.sleep((long)(duration * 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                audioTrack.release();
                isPlaying = false;
                Log.i("playSound", "isPlaying:" + id + ":" + isPlaying);
            }
            catch (IllegalStateException exception) {
                isPlaying = false;
            }

        }
    }
}
