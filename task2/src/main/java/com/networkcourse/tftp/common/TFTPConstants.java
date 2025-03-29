package com.networkcourse.tftp.common;

/**
 * Constants for the TFTP protocol over TCP.
 */
public class TFTPConstants {
    // Default port
    public static final int DEFAULT_PORT = 6969;
    
    // Operation codes (similar to UDP TFTP, but simplified)
    public static final short OP_RRQ = 1;      // Read request
    public static final short OP_WRQ = 2;      // Write request
    public static final short OP_DATA = 3;     // Data packet
    public static final short OP_ERROR = 5;    // Error
    
    // Protocol constants
    public static final int MAX_DATA_SIZE = 512;
    public static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + 4; // 4 bytes for opcode and block number
    public static final int HEADER_SIZE = 4;  // opcode (2 bytes) + block number (2 bytes)
    
    // Transfer modes
    public static final String MODE_OCTET = "octet";
    
    // Error codes
    public static final short ERR_NOT_DEFINED = 0;
    public static final short ERR_FILE_NOT_FOUND = 1;
    public static final short ERR_ACCESS_VIOLATION = 2;
    public static final short ERR_DISK_FULL = 3;
    public static final short ERR_ILLEGAL_OP = 4;
    public static final short ERR_FILE_EXISTS = 6;
    
    // Error messages
    public static final String ERR_MSG_FILE_NOT_FOUND = "File not found.";
    public static final String ERR_MSG_ACCESS_VIOLATION = "Access violation.";
    public static final String ERR_MSG_DISK_FULL = "Disk full or allocation exceeded.";
    public static final String ERR_MSG_ILLEGAL_OP = "Illegal TFTP operation.";
    public static final String ERR_MSG_FILE_EXISTS = "File already exists.";
}
