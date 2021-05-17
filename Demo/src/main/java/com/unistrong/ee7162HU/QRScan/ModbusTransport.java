package com.unistrong.ee7162HU.QRScan;

import java.io.IOException;

/**
 * This interface represents basic operation for interaction with devices
 * through the various interface.
 *
 * @author stanislav.kleinikov@gmail.com
 */
public interface ModbusTransport {

    /**
     * Sends the message to connected device
     *
     * @param message to send
     */
    void sendMessage(byte[] message) throws IOException;

    /**
     * Allows to receive a message from connected device
     *
     * @return the byte array that contains a response
     */
    byte[] receiveMessage();

    /**
     * Establish the connection with a device
     */
    boolean connect();

    /**
     * Closes an existing connection and releases any resources
     */
    void close();

    /**
     * Allows to know if the connection is established in this moment.
     *
     * @return <code>true</code> if the connection is established and <code>false</code> otherwise.
     */
    boolean isConnected();
}
