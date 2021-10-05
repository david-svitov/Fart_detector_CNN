package com.example.fartdetector;

import android.util.Log;

import java.util.Arrays;

/***
 * Threadsafe implementation of the ring buffer to store audio signal
 */
public class RingAudioBuffer {

    private float[] floatArray;
    private final Object lock = new Object();

    /***
     * Create buffer array of floats with fixed size
     * @param bufferSize size of buffer
     */
    public RingAudioBuffer(int bufferSize) {
        floatArray = new float[bufferSize];
    }

    /***
     * Push new values to ring buffer. Values are pushed from the end of the buffer.
     * Oldest values are erased from the buffer.
     * Get short values and normalize them to float
     * @param newValues Array of new values to push into buffer
     * @param max_value Maximum possible value of the signal, to normalize signal
     */
    public void push(short[] newValues, short max_value) {
        int buffer_offset = newValues.length;

        // Process cases when length of new array is bigger then buffer size
        if (buffer_offset > floatArray.length) {
            newValues = Arrays.copyOfRange(newValues,
                    buffer_offset- floatArray.length,
                    buffer_offset);
            buffer_offset = newValues.length;
        }

        synchronized (lock) {
            for (int i = 0; i < floatArray.length - buffer_offset; i++) {
                floatArray[i] = floatArray[i + buffer_offset];
            }

            for (int i = 0; i < buffer_offset; i++) {
                floatArray[floatArray.length - buffer_offset + i] = (float) newValues[i] / max_value;
            }
        }
    }

    /***
     * Return current state of the buffer
     * @return Array with current buffer values
     */
    public float[] getData() {
        synchronized (lock) {
            return floatArray.clone();
        }
    }

    /***
     * Return current state of the buffer. Normalize data to have mean=0 and std=1
     * @return Array with current buffer values
     */
    public float[] getDataNormalized() {
        float[] buffer_state;
        synchronized (lock) {
            buffer_state = floatArray.clone();
        }

        float sum = 0;
        for(int i = 0; i < buffer_state.length; ++i) {
            sum += buffer_state[i];
        }
        float mean_value = sum / buffer_state.length;

        float sq_sum = 0;
        for(int i = 0; i < buffer_state.length; ++i) {
            sq_sum += Math.pow(buffer_state[i] - mean_value, 2);
        }
        float std_value = (float) Math.sqrt(sq_sum / buffer_state.length);

        for(int i = 0; i < buffer_state.length; ++i) {
            buffer_state[i] = (buffer_state[i] - mean_value) / std_value;
        }

        return buffer_state;
    }
}
