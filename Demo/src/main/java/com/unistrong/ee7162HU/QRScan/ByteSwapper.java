package com.unistrong.ee7162HU.QRScan;

public class ByteSwapper {

    public static short swap(short value) {
        int b1 = value & 0xff;
        int b2 = (value >> 8) & 0xff;
        return (short) (b1 << 8 | b2);
    }

}
