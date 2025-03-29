package com.networkcourse.tftp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Base class for TFTP packet structures.
 * Handles common packet operations and formatting.
 */
public abstract class TFTPPacket {
    protected short opcode;
    
    protected TFTPPacket(short opcode) {
        this.opcode = opcode;
    }
    
    /**
     * Gets the opcode of this packet.
     * @return The opcode value
     */
    public short getOpcode() {
        return opcode;
    }
    
    /**
     * Serializes this packet into a byte array for transmission.
     * @return Byte array representation of the packet
     * @throws IOException If an error occurs during serialization
     */
    public abstract byte[] serialize() throws IOException;
    
    /**
     * Helper method to write null-terminated string to ByteArrayOutputStream.
     * @param baos The ByteArrayOutputStream
     * @param str The string to write
     * @throws IOException If an error occurs during writing
     */
    protected static void writeNullTerminatedString(ByteArrayOutputStream baos, String str) throws IOException {
        baos.write(str.getBytes(StandardCharsets.US_ASCII));
        baos.write(0); // Null terminator
    }
    
    /**
     * Factory method to create a packet from a received byte array.
     * @param data The received data
     * @param length The length of the data
     * @return The appropriate packet object
     * @throws IOException If the packet data is invalid
     */
    public static TFTPPacket createFromBytes(byte[] data, int length) throws IOException {
        if (length < 2) {
            throw new IOException("Invalid packet: too short");
        }
        
        // Extract opcode from first two bytes
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        short opcode = buffer.getShort();
        
        switch (opcode) {
            case TFTPConstants.OP_RRQ:
            case TFTPConstants.OP_WRQ:
                return TFTPRequestPacket.createFromBuffer(buffer, opcode);
            case TFTPConstants.OP_DATA:
                return TFTPDataPacket.createFromBuffer(buffer);
            case TFTPConstants.OP_ACK:
                return TFTPAckPacket.createFromBuffer(buffer);
            case TFTPConstants.OP_ERROR:
                return TFTPErrorPacket.createFromBuffer(buffer);
            default:
                throw new IOException("Unknown opcode: " + opcode);
        }
    }
}
