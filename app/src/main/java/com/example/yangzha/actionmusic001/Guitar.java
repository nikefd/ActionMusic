package com.example.yangzha.actionmusic001;

import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Guitar extends AppCompatActivity implements SensorEventListener {

    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final int sampleRate = 48000;

    private boolean isPlaying = false;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;
    float[] aValues = new float[3];
    float[] mValues = new float[3];
    float[] angles = new float[3];

    private TextView chordText, anglesText;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guitar);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        anglesText = (TextView) findViewById(R.id.magneticField);
        chordText = (TextView) findViewById(R.id.chordSelect);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Choose tone
        String[] tones = new String[]{"1", "2", "3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onStop() {
        super.onStop();
        // 取消监听
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mValues = event.values;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            aValues = event.values;

            angles = calculateOrientation();
            String mText = "x:" + angles[0] + ", y:" + angles[1] + ", z:" + angles[2];
            anglesText.setText(mText);

            if (!isPlaying && (Math.abs(aValues[0]) + Math.abs(aValues[1]) + Math.abs(aValues[2])) > 20) {
                if(angles[2] > 0 && angles[2] < 36) play("guitar_c");
                else if(angles[2] > 36 && angles[2] < 72) play("guitar_g");
                else if(angles[2] > 72 && angles[2] < 108) play("guitar_am");
                else if(angles[2] > 108 && angles[2] < 144) play("guitar_em");
                else if(Math.abs(angles[2]) > 144) play("guitar_f");
            }
        }
    }

    private float[] calculateOrientation()
    {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, aValues, mValues);
        SensorManager.getOrientation(R, values);
        values[0] = (float) Math.toDegrees(values[0]);
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
        return values;
    }

    private void play(String chordName)
    {
        String id = UUID.randomUUID().toString();
        isPlaying = true;
        chordText.setText(chordName);
        Log.i("playSound", id + chordName);
        PlayThread thread_playNote = new PlayThread(chordName, 0.1, id);
        thread_playNote.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class PlayThread extends Thread {
        String noteName;
        double duration;  //ms
        String id;

        private PlayThread(String noteName, double duration, String id) {
            this.noteName = noteName;
            this.duration = duration;
            this.id = id;
        }

        @Override
        public void run() {
            int numSamples = (int)(duration * sampleRate);
            byte[] data = new byte[sampleRate * 2];

            Log.i("Success", aValues[0] + " " + aValues[1] + " " + aValues[2]);
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, data.length, AudioTrack.MODE_STATIC);
            String filename = noteName + ".wav";


            try {
                InputStream fileInputStream = getAssets().open(filename);
                DataInputStream dataInputStream = new DataInputStream(fileInputStream);

                dataInputStream.read(data, 0, data.length);
                audioTrack.write(data, 0, data.length);
                audioTrack.play();
                try {
                    Thread.sleep((long)(duration * 1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isPlaying = false;
                if (duration * 1000 < 2000) {
                    try {
                        Thread.sleep((long)(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                audioTrack.stop();
                audioTrack.release();
                dataInputStream.close();
                fileInputStream.close();
            } catch (IOException e)
            {
                e.printStackTrace();
                isPlaying = false;
            }
        }
    }
}
