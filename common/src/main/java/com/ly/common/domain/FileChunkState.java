package com.ly.common.domain;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class FileChunkState {

    private final AtomicInteger count = new AtomicInteger();
    private final int size;
    private byte[] states;

    public FileChunkState(int size) {
        this.size = size;
        if (size > 0) {
            states = new byte[(size + 7) / 8 + 4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(states);
            byteBuffer.putInt(size);
        }
        count.set(0);
    }

    public FileChunkState(byte[] states) {
        this.states = states;
        if (states != null && states.length >= 4) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(states);
            size = byteBuffer.getInt();
            int c = 0;
            for (int i = 4; i < states.length; i++) {
                c += binaryOneCount(states[i]);
            }
            count.set(c);
        } else {
            size = 0;
            count.set(0);
        }
    }

    private int binaryOneCount(byte b) {
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) == 1) {
                count++;
            }
            b = (byte) (b >>> 1);
        }
        return count;
    }

    public int getState(int index) {
        if (index < 0 || index >= size) {
            return -1;
        }
        int i = index / 8 + 4;
        int o = index % 8 + 4;
        if (i >= states.length) {
            return -1;
        }
        return (states[i] >>> o) & 1;
    }

    public synchronized int setStateAndCount(int index, boolean state) {
        if (state) {
            return setStateAndCount(index);
        }
        if (index < 0 || index >= size) {
            return -1;
        }
        int o = index % 8 + 4;
        int i = index / 8 + 4;
        if (i >= states.length) {
            return -1;
        }
        if (((states[i] >>> o) & 1) > 0) {
            states[i] &= ~(1 << o);
            return count.addAndGet(-1);
        } else {
            return getCount();
        }
    }

    public synchronized int setStateAndCount(int index) {
        int s = getState(index);
        if (s == -1) {
            return -1;
        } else if (s == 1) {
            return getCount();
        } else {
            int i = index / 8;
            int o = index % 8;
            states[i] |= 1 << o;
            return incrementCountAndGet();
        }
    }

    public int getCount() {
        return count.get();
    }

    public byte[] getStates() {
        return states;
    }

    public int incrementCountAndGet() {
        return count.incrementAndGet();
    }

    public int plusCountAndGet(int plus) {
        return count.addAndGet(plus);
    }
}
