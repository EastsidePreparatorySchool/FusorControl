/**
 * A class to represent a MAC address
 * @author Arjun VedBrat
 * @date 11/30/2021
 */

package com.eastsideprep.serialdevice;

/**
 * Refers to a MAC address [of a device]
 */
public class MACAddress {
    private byte[] address = new byte[6];

    public MACAddress() { }

    public MACAddress(byte[] address) {
        this.setMACAddress(address);
    }

    /**
     * Sets this object's MAC address to {@code address}
     * @param address The MAC address
     */
    public void setMACAddress(byte[] address) {
        this.address = address;
    }

    /**
     * Gets the MAC address as a {@code byte[]}
     * @return The MAC address as a {@code byte[]}
     */
    public byte[] asByteArray() {
        return this.address;
    }
}
