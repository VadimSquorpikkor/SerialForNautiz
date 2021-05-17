package com.unistrong.ee7162HU.QRScan;

public class BitConverter {

    public static byte[] getBytes(short v) {
        byte[] writeBuffer = new byte[2];
        writeBuffer[0] = (byte) ((v) & 0xFF);
        writeBuffer[1] = (byte) ((v >>> 8) & 0xFF);
        return writeBuffer;
    }

    public static short toInt16(byte[] data, int offset) {
        return (short) (data[offset] & 0xFF | (data[offset + 1] & 0xFF) << 8);
    }
}
