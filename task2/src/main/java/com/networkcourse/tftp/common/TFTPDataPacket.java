package com.networkcourse.tftp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents a TFTP Data packet (DATA).
 * Format:
 *    2 bytes     2 bytes      n bytes
 *   ----------------------------------
 *  | Opcode |   Block #  |   Data     |
 *   ----------------------------------
 */
public class TFTPDataPacket extends TFTPPacket {
    private final short blockNumber;
    private final byte[] data;
    private final int dataLength;
    
    /**
     * Creates a new DATA packet.
     * 
     * @param blockNumber The block number (1-65535)
     * @param data The data to send
     * @param dataLength The length of data
     */
    public TFTPDataPacket(short blockNumber, byte[] data, int dataLength) {
        super(TFTPConstants.OP_DATA);
        
        if (dataLength > TFTPConstants.MAX_DATA_SIZE) {
            throw new IllegalArgumentException("Data length exceeds maximum allowed size");
        }
        
        this.blockNumber = blockNumber;
        this.data = Arrays.copyOf(data, dataLength);
        this.dataLength = dataLength;
    }
    
    /**
     * Get the block number of this data packet.
     * @return The block number
     */
    public short getBlockNumber() {
        return blockNumber;
    }
    
    /**
     * Get the data contained in this packet.
     * @return The data as a byte array
     */
    public byte[] getData() {
        return Arrays.copyOf(data, dataLength);
    }
    
    /**
     * Get the length of data in this packet.
     * @return The data length
     */
    public int getDataLength() {
        return dataLength;
    }
    
    /**
     * Checks if this is the final data packet in a transfer.
     * The last packet is identified by having less than the maximum data size.
     * 
     * @return true if this is the final packet, false otherwise
     */
    public boolean isFinalPacket() {
        return dataLength < TFTPConstants.MAX_DATA_SIZE;
    }
    
    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(dataLength + 4);
        
        // Write opcode
        baos.write((opcode >> 8) & 0xFF);
        baos.write(opcode & 0xFF);
        
        // Write block number
        baos.write((blockNumber >> 8) & 0xFF);
        baos.write(blockNumber & 0xFF);
        
        // Write data
        baos.write(data, 0, dataLength);
        
        return baos.toByteArray();
    }
    
    /**
     * Factory method to create a data packet from a byte buffer.
     * 
     * @param buffer The buffer containing the packet data (positioned after the opcode)
     * @return A new TFTPDataPacket
     * @throws IOException If the packet data is invalid
     */
    public static TFTPDataPacket createFromBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < 2) {
            throw new IOException("Invalid data packet: missing block number");
        }
        
        short blockNumber = buffer.getShort();
        
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        
        return new TFTPDataPacket(blockNumber, data, data.length);
    }
}
