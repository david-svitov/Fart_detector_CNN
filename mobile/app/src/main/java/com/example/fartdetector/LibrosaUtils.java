package com.example.fartdetector;

import static java.lang.Math.log10;
import static java.lang.Math.max;

import android.util.Log;
import android.util.Pair;

public class LibrosaUtils {

    /***
     * Find minimum and maximum values on a spectrogram
     * @param spectrogram Two dimensions array with spectrogram values
     * @return Paired minimum and maximum values
     */
    public static Pair<Float, Float> getMinMaxValues(float[][] spectrogram){
        float max_value = Float.MIN_VALUE;
        float min_value = Float.MAX_VALUE;
        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                float value = spectrogram[i][j];
                if (value > max_value) max_value = value;
                if (value < min_value) min_value = value;
            }
        }

        Pair<Float, Float> valuesMinMax = new Pair<>(min_value, max_value);
        return valuesMinMax;
    }

    /***
     * Normalize spectrogram values from 0 to 1 by min and max
     * @param spectrogram Two dimensions array with spectrogram values
     * @return Normalized spectrogram
     */
    public static float[][] normalizeSpectrogramMinMax(float[][] spectrogram) {
        Pair<Float, Float> valuesMinMax = LibrosaUtils.getMinMaxValues(spectrogram);
        float min_value = valuesMinMax.first;
        float max_value = valuesMinMax.second;

        float[][] newSpectrogram = new float[spectrogram.length][spectrogram[0].length];

        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                newSpectrogram[i][j] = (spectrogram[i][j] - min_value) / (max_value - min_value);
            }
        }
        return newSpectrogram;
    }

    /***
     * Normalize spectrogram by global mean and std values
     * @param spectrogram Two dimensions array with spectrogram values
     * @return Normalized spectrogram
     */
    public static float[][] normalizeSpectrogramMeanStd(float[][] spectrogram) {
        float mean_value = -38.598f;
        float std_value = 12.22f;

        float[][] newSpectrogram = new float[spectrogram.length][spectrogram[0].length];

        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                newSpectrogram[i][j] = (spectrogram[i][j] - mean_value) / std_value;
            }
        }

        return newSpectrogram;
    }

    /***
     * Convert spectrogram values to the Decibels in calculation stable way
     * @param spectrogram Two dimensions array with spectrogram values
     */
    public static void powerToDb(float[][] spectrogram) {
        Pair<Float, Float> valuesMinMax = LibrosaUtils.getMinMaxValues(spectrogram);
        float max_value = valuesMinMax.second;
        float top_db = 80.0f;
        double amin = 1e-10f;

        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                float value = spectrogram[i][j];
                spectrogram[i][j]  = (float) (10 * log10(max(value, amin)) - 10 * log10(max(max_value, amin)));
            }
        }

        valuesMinMax = LibrosaUtils.getMinMaxValues(spectrogram);
        max_value = valuesMinMax.second;

        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                spectrogram[i][j] = max(spectrogram[i][j], max_value - top_db);
            }
        }
    }
}
