package com.unistrong.ee7162HU.QRScan;

import android.content.SharedPreferences;
import android.util.Log;

import com.handheldgroup.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

public class PortApplication extends android.app.Application {
    private SerialPort mSerialPort = null;
    private SerialPortFinder mSerialPortFinder = null;

    public SerialPortFinder getPortFinder() {
        if (mSerialPortFinder == null) {
            mSerialPortFinder = new SerialPortFinder();
        }

        return mSerialPortFinder;
    }

    public void setProfile(String path, int baud) {
        closeSerialPort();
        SharedPreferences sp = getSharedPreferences("device.demo.serial_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("DEVICE", path);
        editor.putString("BAUDRATE", String.valueOf(baud));
        editor.commit();
    }

    public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
            SharedPreferences sp = getSharedPreferences("device.demo.serial_preferences", MODE_PRIVATE);
            String path = sp.getString("DEVICE", "");
            int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));


//            Log.e("TAG", "getSerialPort: BAUDRATE = "+baudrate);
//            Log.e("TAG", "getSerialPort: PATH = "+path);


            if ((path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
        }

        return mSerialPort;
    }

    public void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }
}