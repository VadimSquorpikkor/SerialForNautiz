package com.unistrong.ee7162HU.QRScan;

import java.nio.ByteBuffer;

public class mbs {

    public static byte[] getMessageWithCRC16(byte[] bytes, boolean crcOrder) {
        short crc = (short) calcCRC(bytes);
        if (crcOrder) {
            crc = ByteSwapper.swap(crc);
        }
        byte[] bytesCRC = BitConverter.getBytes(crc);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + bytesCRC.length);
        buffer.put(bytes);
        buffer.put(bytesCRC);
        return buffer.array();
    }


    public static int calcCRC(byte[] dataBuffer) {
        int sum = 0xffff;
        for (byte aDataBuffer : dataBuffer) {
            sum = (sum ^ (aDataBuffer & 255));
            for (int j = 0; j < 8; j++) {
                if ((sum & 0x1) == 1) {
                    sum >>>= 1;
                    sum = (sum ^ 0xA001);
                } else {
                    sum >>>= 1;
                }
            }
        }
        return sum;
    }



    public static final int DEFAULT_MESSAGE_LENGTH = 2;
    public static final byte READ_DEVICE_ID = 0x11;
    public static final byte ADDRESS = 0x02;

    public static boolean getCRCOrder() {
        return true;
    }


}
