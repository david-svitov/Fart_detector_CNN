package com.example.fartdetector;

import junit.framework.TestCase;
import static org.junit.Assert.*;

public class RingAudioBufferTest extends TestCase {

    public void testCreation() {
        RingAudioBuffer buffer = new RingAudioBuffer(4);
        assertArrayEquals("Arrays are not equal", new float[]{0, 0, 0, 0}, buffer.getData(), (float) 0.01);
    }

    public void testPushingSingleElement() {
        RingAudioBuffer buffer = new RingAudioBuffer(4);
        buffer.push(new short[]{9}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{0, 0, 0, 9}, buffer.getData(), (float) 0.01);
    }

    public void testManyPushingSingleElement() {
        RingAudioBuffer buffer = new RingAudioBuffer(4);
        buffer.push(new short[]{9}, (short) 1);
        buffer.push(new short[]{8}, (short) 1);
        buffer.push(new short[]{7}, (short) 1);
        buffer.push(new short[]{6}, (short) 1);
        buffer.push(new short[]{5}, (short) 1);
        buffer.push(new short[]{4}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{7, 6, 5, 4}, buffer.getData(), (float) 0.01);
    }

    public void testPushingArray() {
        RingAudioBuffer buffer = new RingAudioBuffer(4);
        buffer.push(new short[]{9, 8}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{0, 0, 9, 8}, buffer.getData(), (float) 0.01);
        buffer.push(new short[]{9, 8, 7, 6}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{9, 8, 7, 6}, buffer.getData(), (float) 0.01);
        buffer.push(new short[]{9, 8, 7, 6, 5, 4}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{7, 6, 5, 4}, buffer.getData(), (float) 0.01);
    }

    public void testManyPushingArray() {
        RingAudioBuffer buffer = new RingAudioBuffer(4);
        buffer.push(new short[]{9, 8}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{0, 0, 9, 8}, buffer.getData(), (float) 0.01);
        buffer.push(new short[]{7, 6}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{9, 8, 7, 6}, buffer.getData(), (float) 0.01);
        buffer.push(new short[]{5, 4, 3}, (short) 1);
        assertArrayEquals("Arrays are not equal", new float[]{6, 5, 4, 3}, buffer.getData(), (float) 0.01);
    }
}