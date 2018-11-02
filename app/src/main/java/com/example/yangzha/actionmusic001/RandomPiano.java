package com.example.yangzha.actionmusic001;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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
import java.util.UUID;

public class RandomPiano extends AppCompatActivity implements SensorEventListener {

    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
    // and modified by Steve Pomeroy <steve@staticfree.info>
    private final int sampleRate = 8000;

    private boolean isPlaying = false;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    float[] aValues = new float[3];
    float[] mValues = new float[3];
    int toneSelect = 4;

    int noteArrayLen = 50;
    String[] noteArray = new String[noteArrayLen];
    int cur = 0;

    private TextView accelerometerText, noteText, toneSelectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        accelerometerText = (TextView) findViewById(R.id.accelerometer);
        noteText = (TextView) findViewById(R.id.note);
        toneSelectText = (TextView) findViewById(R.id.toneSelect);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Choose tone
        String[] tones = new String[]{"1", "2", "3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = super.findViewById(R.id.tone);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                toneSelect = arg2 + 1;
                toneSelectText.setText("You have choose tone " + toneSelect);
            }
            public void onNothingSelected(AdapterView<?> arg0) { }
        });
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

            String aText = "x:" + aValues[0] + ", y:" + aValues[1] + ", z:" + aValues[2];
            accelerometerText.setText(aText);

            if (!isPlaying) {  // up down left right front back
                //screen up
                if(aValues[2] >= 12 && aValues[0] < 2 && aValues[1] < 2) play("e" + toneSelect);  //down
                else if(aValues[2] >= 8 && aValues[0] >= 4 && aValues[1] < 2) play("f" + toneSelect); //right
                else if(aValues[2] >= 8 && aValues[1] >= 4 && aValues[0] < 2) play("g" + toneSelect); //front
                    //screen down
                else if (aValues[2] <= -14 && aValues[0] < 2 && aValues[1] < 2) play("a" + toneSelect);
                else if(aValues[2] <= -8 && aValues[0] >= 4 && aValues[1] < 2) play("b" + toneSelect); //right
                else if(aValues[2] <= -8 && aValues[1] >= 4 && aValues[0] < 2) play("c" + (toneSelect + 1)); //front
                    //screen left
                else if (aValues[0] >= 12 && aValues[1] < 2 && aValues[2] < 2) play("d" + (toneSelect + 1));
                else if(aValues[0] >= 8 && aValues[2] <= -4 && aValues[1] < 2) play("e" + (toneSelect + 1)); //right
                else if(aValues[0] >= 8 && aValues[1] >= 4 && aValues[2] < 2) play("f" + (toneSelect + 1)); //front
                    //screen right
                else if (aValues[0] <= -14 && aValues[1] < 2 && aValues[2] < 2) play("g" + (toneSelect + 1));
                else if(aValues[0] <= -8 && aValues[2] >= 4 && aValues[1] < 2) play("a" + (toneSelect + 1)); //right
                else if(aValues[0] <= -8 && aValues[1] >= 4 && aValues[2] < 2) play("b" + (toneSelect + 1)); //front
                    //telephone upright
                else if (aValues[1] >= 12 && aValues[0] < 2 && aValues[2] < 2) play("c" + (toneSelect + 2));
                else if(aValues[1] >= 8 && aValues[0] >= 4 && aValues[2] < 2) play("d" + (toneSelect + 2)); //right
                else if(aValues[1] >= 8 && aValues[2] <= -4 && aValues[0] < 2) play("e" + (toneSelect + 2)); //front
                    //telephone handstand
                else if (aValues[1] <= -14 && aValues[0] < 2 && aValues[2] < 2) play("f" + (toneSelect + 2));
                else if(aValues[1] <= -8 && aValues[0] >= 4 && aValues[2] < 2) play("g" + (toneSelect + 2)); //right
                else if(aValues[1] <= -8 && aValues[2] >= 4 && aValues[0] < 2) play("a" + (toneSelect + 2)); //front
            }
        }
    }

    private void play(String noteName)
    {
        String id = UUID.randomUUID().toString();
        isPlaying = true;
        noteArray[cur%noteArrayLen] = noteName;
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<noteArrayLen; ++i) {
            if (noteArray[(i + cur + 1) % noteArrayLen] != null) {
                builder.append(noteArray[(i + cur + 1) % noteArrayLen]);
            }
        }
        cur++;
        String NoteStream = builder.toString();
        noteText.setText(NoteStream);

        Log.i("playSound", id + noteName);
        PlayThread thread_playNote = new PlayThread(noteName, 0.25, id);
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
            }
        }
    }
}
