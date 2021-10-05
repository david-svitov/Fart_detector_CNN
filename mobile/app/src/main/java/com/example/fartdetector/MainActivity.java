package com.example.fartdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.fartdetector.databinding.ActivityMainBinding;
import com.example.fartdetector.ml.FartDetector;
import com.jlibrosa.audio.JLibrosa;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    // Sound settings
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int BYTES_PER_ELEMENT = 2;
    private static final int SECONDS_TO_PROCESS = 2;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    // Spectrogram settings
    private static final int N_FFT = 1024;
    private static final int N_MELS = 128;
    private static final int HOP_LENGTH = 128;

    // Sound thread variable
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int bufferSize = 0;
    private RingAudioBuffer buffer;

    // Spectrogram visualization
    private Timer redrawTimer;
    private JLibrosa librosa = new JLibrosa();
    private static final int REDRAW_INTERVAL = 200; // ms

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {
            Manifest.permission.RECORD_AUDIO
    };

    // Fart detector
    private static final int MODEL_INPUT_W = 251;
    private static final int MODEL_INPUT_H = 128;
    private static final float DETECTION_THRESHOLD = 0.8f;
    FartDetector tfliteModel;
    TensorBuffer inputFeature0;

    // Bluetooth
    private HCConnector bluetoothSpray;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            bluetoothSpray = new HCConnector();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }

        buffer = new RingAudioBuffer(RECORDER_SAMPLERATE * SECONDS_TO_PROCESS);

        try {
            tfliteModel = FartDetector.newInstance(getApplicationContext());
            inputFeature0 = TensorBuffer.createFixedSize(
                    new int[]{1, MODEL_INPUT_H, MODEL_INPUT_W, 1},
                    DataType.FLOAT32
            );
        } catch (IOException e) {
            Log.e("TFLite model", "Can't load tflite model");
            e.printStackTrace();
        }

        // Setup time to redraw spectrogram
        redrawTimer = new Timer();
        redrawTimer.scheduleAtFixedRate(new TimerTask() {
            private int showTitleTimer = 0;

            @Override
            public void run() {
                // Get and redraw spectrogram
                float[][] spectrogram = spectrogramUpdate();
                float[] flattenedSpectrogam = flattenSpectrogram(spectrogram);

                inputFeature0.loadArray(flattenedSpectrogam);

                // Runs model inference and gets result.
                FartDetector.Outputs outputs = tfliteModel.process(inputFeature0);
                TensorBuffer fartPrediction = outputs.getOutputFeature0AsTensorBuffer();

                float score = fartPrediction.getFloatValue(0);
                // Send signal to the spray
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((ProgressBar)findViewById(R.id.fartometr)).setProgress((int) (score * 100));
                        if (score> DETECTION_THRESHOLD) {
                            if (showTitleTimer == 0) {
                                findViewById(R.id.fartAlarm).setVisibility(View.VISIBLE);
                                try {
                                    bluetoothSpray.sendValue("1");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            showTitleTimer = 5;
                        }

                        if (showTitleTimer > 0) {
                            showTitleTimer -= 1;
                        }
                        if (showTitleTimer == 0) {
                            findViewById(R.id.fartAlarm).setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        }, REDRAW_INTERVAL, REDRAW_INTERVAL);

        // Get minimal buffer size
        bufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING);
    }

    /**
     * Function to activate and deactivate buttons
     */
    private void enableButton(int id, boolean isEnable) {
        findViewById(id).setEnabled(isEnable);
    }

    /**
     * Start button click event;
     * Deactivate Start button and activate Stop button
     */
    public void onStartClick(View v) {
        enableButton(R.id.btnStart, false);
        enableButton(R.id.btnStop, true);
        startRecording();
    }

    /**
     * Stop button click event;
     * Deactivate Stop button and activate Start button
     */
    public void onStopClick(View v) {
        enableButton(R.id.btnStart, true);
        enableButton(R.id.btnStop, false);
        stopRecording();
    }

    /**
     * Callback for permission request
     * Check if we have audio record permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    /**
     * Start processing audio from microphone
     * Create audio recorder and separate thread to process audio
     */
    private void startRecording() {

        // Check if we can record audio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                    getApplicationContext(),
                    "Permission for audio record not granted!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Create audio recorder
        recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(RECORDER_AUDIO_ENCODING)
                        .setSampleRate(RECORDER_SAMPLERATE)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        recorder.startRecording();
        isRecording = true;

        // Start thread with processing recorded files
        recordingThread = new Thread(new Runnable() {
            public void run() {
                processAudioData();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    /**
     * Stops the recording activity
     */
    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    /***
     * Function to run in audio thread.
     * This function get next chunk of data from audio device and push it to the ring buffer
     */
    private void processAudioData() {
        // Chunk of data from microphone
        short dataShort[] = new short[bufferSize / BYTES_PER_ELEMENT];

        while (isRecording) {
            // Get next chunk of data from microphone
            recorder.read(dataShort, 0, bufferSize / BYTES_PER_ELEMENT);
            buffer.push(dataShort, Short.MAX_VALUE);
        }
    }

    /***
     * This one is called by timer. Build spectrogram and draw it on the screen
     * @return Two dimensional array with spectrogram
     */
    public float[][] spectrogramUpdate() {
        // Update spectrogram
        float[][] spectrogram = librosa.generateMelSpectroGram(
                buffer.getDataNormalized(),
                RECORDER_SAMPLERATE, N_FFT, N_MELS, HOP_LENGTH);
        LibrosaUtils.powerToDb(spectrogram);

        float[][] normalizedSpec = LibrosaUtils.normalizeSpectrogramMeanStd(spectrogram);

        ((SpectrogramView)findViewById(R.id.spectrogramView)).setSpectrogram(spectrogram);
        findViewById(R.id.spectrogramView).postInvalidate();
        return normalizedSpec;
    }

    /**
     * Flatten a spectrogram for tflite processing
     * @param spectrogram Two dimensions array with spectrogram values
     * @return One dimension array with flattened spectrogram
     */
    public float[] flattenSpectrogram(float[][] spectrogram) {
        float[] flattenedSpectrogam = new float[MODEL_INPUT_H * MODEL_INPUT_W];
        if (spectrogram.length != MODEL_INPUT_H && spectrogram[0].length != MODEL_INPUT_W) {
            throw new IllegalArgumentException("Incorrect spectrogram size");
        }

        for (int i = 0; i < MODEL_INPUT_H; ++i) {
            for (int j = 0; j < MODEL_INPUT_W; ++j) {
                flattenedSpectrogam[i * MODEL_INPUT_W + j] = spectrogram[i][j];
            }
        }

        return flattenedSpectrogam;
    }
}
