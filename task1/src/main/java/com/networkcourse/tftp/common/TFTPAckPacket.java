package com.networkcourse.tftp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a TFTP Acknowledgment packet (ACK).
 * Format:
 *    2 bytes     2 bytes
 *   ---------------------
 *  | Opcode |   Block #  |
 *   ---------------------
 */
public class TFTPAckPacket extends TFTPPacket {
    private final short blockNumber;
    
    /**
     * Creates a new ACK packet.
     * 
     * @param blockNumber The block number being acknowledged
     */
    public TFTPAckPacket(short blockNumber) {
        super(TFTPConstants.OP_ACK);
        this.blockNumber = blockNumber;
    }
    
    /**
     * Get the block number being acknowledged.
     * @return The block number
     */
    public short getBlockNumber() {
        return blockNumber;
    }
    
    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        
        // Write opcode
        baos.write((opcode >> 8) & 0xFF);
        baos.write(opcode & 0xFF);
        
        // Write block number
        baos.write((blockNumber >> 8) & 0xFF);
        baos.write(blockNumber & 0xFF);
        
        return baos.toByteArray();
    }
    
    /**
     * Factory method to create an ACK packet from a byte buffer.
     * 
     * @param buffer The buffer containing the packet data (positioned after the opcode)
     * @return A new TFTPAckPacket
     * @throws IOException If the packet data is invalid
     */
    public static TFTPAckPacket createFromBuffer(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < 2) {
            throw new IOException("Invalid ACK packet: missing block number");
        }
        
        short blockNumber = buffer.getShort();
        return new TFTPAckPacket(blockNumber);
    }
}
