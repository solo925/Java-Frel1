package com.networkcourse.tftp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Represents a TFTP Error packet (ERROR).
 * Format:
 *    2 bytes     2 bytes      string    1 byte
 *   -----------------------------------------
 *  | Opcode |  ErrorCode |   ErrMsg   |   0  |
 *   -----------------------------------------
 */
public class TFTPErrorPacket extends TFTPPacket {
    private final short errorCode;
    private final String errorMessage;
    
    /**
     * Creates a new ERROR packet.
     * 
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    public TFTPErrorPacket(short errorCode, String errorMessage) {
        super(TFTPConstants.OP_ERROR);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Get the error code.
     * @return The error code
     */
    public short getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the error message.
     * @return The error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Write opcode
        baos.write((opcode >> 8) & 0xFF);
        baos.write(opcode & 0xFF);
        
        // Write error code
        baos.write((errorCode >> 8) & 0xFF);
        baos.write(errorCode & 0xFF);
        
        // Write error message, null-terminated
        writeNullTerminatedString(baos, errorMessage);
        
        return baos.toByteArray();
    }
    
    /**
     * Factory method to create an ERROR packet from a byte buffer.
     * 
     * @param buffer The buffer containing the packet data (positioned after the opcode)
     * @return A new TFTPErrorPacket
     * @throws IOException If the packet data is invalid
     */
    public static TFTPErrorPacket createFromBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < 2) {
            throw new IOException("Invalid error packet: missing error code");
        }
        
        short errorCode = buffer.getShort();
        
        StringBuilder sb = new StringBuilder();
        byte b;
        
        // Read error message
        while (buffer.hasRemaining() && (b = buffer.get()) != 0) {
            sb.append((char) b);
        }
        
        String errorMessage = sb.toString();
        
        return new TFTPErrorPacket(errorCode, errorMessage);
    }
    
    /**
     * Factory method for creating a "File not found" error packet.
     * 
     * @return A new TFTPErrorPacket for file not found
     */
    public static TFTPErrorPacket fileNotFound() {
        return new TFTPErrorPacket(
                TFTPConstants.ERR_FILE_NOT_FOUND, 
                TFTPConstants.ERR_MSG_FILE_NOT_FOUND);
    }
    
    /**
     * Factory method for creating an "Access violation" error packet.
     * 
     * @return A new TFTPErrorPacket for access violation
     */
    public static TFTPErrorPacket accessViolation() {
        return new TFTPErrorPacket(
                TFTPConstants.ERR_ACCESS_VIOLATION, 
                TFTPConstants.ERR_MSG_ACCESS_VIOLATION);
    }
    
    /**
     * Factory method for creating a "Disk full" error packet.
     * 
     * @return A new TFTPErrorPacket for disk full
     */
    public static TFTPErrorPacket diskFull() {
        return new TFTPErrorPacket(
                TFTPConstants.ERR_DISK_FULL, 
                TFTPConstants.ERR_MSG_DISK_FULL);
    }
    
    /**
     * Factory method for creating an "Illegal operation" error packet.
     * 
     * @return A new TFTPErrorPacket for illegal operation
     */
    public static TFTPErrorPacket illegalOperation() {
        return new TFTPErrorPacket(
                TFTPConstants.ERR_ILLEGAL_OP, 
                TFTPConstants.ERR_MSG_ILLEGAL_OP);
    }
}
