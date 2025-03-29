package com.networkcourse.tftp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Represents a TFTP Read or Write Request packet (RRQ/WRQ).
 * Format:
 *    2 bytes    string    1 byte    string    1 byte
 *   ------------------------------------------------
 *  | Opcode |  Filename  |   0   |    Mode    |   0  |
 *   ------------------------------------------------
 */
public class TFTPRequestPacket extends TFTPPacket {
    private final String filename;
    private final String mode;
    
    /**
     * Creates a new request packet (RRQ or WRQ).
     * 
     * @param opcode The opcode (OP_RRQ or OP_WRQ)
     * @param filename The filename to read or write
     * @param mode The transfer mode (should be "octet" for this implementation)
     */
    public TFTPRequestPacket(short opcode, String filename, String mode) {
        super(opcode);
        
        if (opcode != TFTPConstants.OP_RRQ && opcode != TFTPConstants.OP_WRQ) {
            throw new IllegalArgumentException("Invalid opcode for request packet");
        }
        
        this.filename = filename;
        this.mode = mode;
    }
    
    /**
     * Get the filename from this request.
     * @return The filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Get the mode from this request.
     * @return The mode
     */
    public String getMode() {
        return mode;
    }
    
    /**
     * Checks if this is a read request (RRQ).
     * @return true if this is a read request, false otherwise
     */
    public boolean isReadRequest() {
        return getOpcode() == TFTPConstants.OP_RRQ;
    }
    
    /**
     * Checks if this is a write request (WRQ).
     * @return true if this is a write request, false otherwise
     */
    public boolean isWriteRequest() {
        return getOpcode() == TFTPConstants.OP_WRQ;
    }
    
    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Write opcode
        outputStream.write((getOpcode() >> 8) & 0xFF);
        outputStream.write(getOpcode() & 0xFF);
        
        // Write filename
        outputStream.write(filename.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(0);  // Zero byte separator
        
        // Write mode
        outputStream.write(mode.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(0);  // Zero byte separator
        
        return outputStream.toByteArray();
    }
    
    /**
     * Creates a request packet from raw bytes.
     * 
     * @param opcode The opcode of the packet
     * @param data The raw bytes of the packet (without the opcode)
     * @param length The length of the data
     * @return A new request packet
     */
    public static TFTPRequestPacket parseFrom(short opcode, byte[] data, int length) {
        // Find the first zero byte (end of filename)
        int i = 0;
        while (i < length && data[i] != 0) {
            i++;
        }
        
        if (i >= length) {
            throw new IllegalArgumentException("Invalid request packet: no zero after filename");
        }
        
        // Extract filename
        String filename = new String(data, 0, i, StandardCharsets.US_ASCII);
        
        // Move past the zero byte
        i++;
        
        // Find the second zero byte (end of mode)
        int j = i;
        while (j < length && data[j] != 0) {
            j++;
        }
        
        if (j >= length) {
            throw new IllegalArgumentException("Invalid request packet: no zero after mode");
        }
        
        // Extract mode
        String mode = new String(data, i, j - i, StandardCharsets.US_ASCII);
        
        return new TFTPRequestPacket(opcode, filename, mode);
    }
}
