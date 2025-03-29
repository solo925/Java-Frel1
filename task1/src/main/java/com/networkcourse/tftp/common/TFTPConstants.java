package com.networkcourse.tftp.common;

/**
 * Constants for the TFTP protocol as specified in RFC 1350.
 */
public class TFTPConstants {
    // TFTP opcodes
    public static final short OP_RRQ = 1;      // Read request
    public static final short OP_WRQ = 2;      // Write request
    public static final short OP_DATA = 3;     // Data packet
    public static final short OP_ACK = 4;      // Acknowledgment
    public static final short OP_ERROR = 5;    // Error message
    
    // TFTP error codes
    public static final short ERR_NOT_DEFINED = 0;       // Not defined
    public static final short ERR_FILE_NOT_FOUND = 1;    // File not found
    public static final short ERR_ACCESS_VIOLATION = 2;  // Access violation
    public static final short ERR_DISK_FULL = 3;         // Disk full
    public static final short ERR_ILLEGAL_OP = 4;        // Illegal TFTP operation
    public static final short ERR_UNKNOWN_TID = 5;       // Unknown transfer ID
    public static final short ERR_FILE_EXISTS = 6;       // File already exists
    public static final short ERR_NO_SUCH_USER = 7;      // No such user
    
    // Mode strings
    public static final String MODE_OCTET = "octet";
    
    // Protocol constants
    public static final int DEFAULT_PORT = 6969;  // Using port > 1024 as required
    public static final int MAX_DATA_SIZE = 512;  // Max data size per packet
    public static final int MAX_PACKET_SIZE = MAX_DATA_SIZE + 4;  // Data + opcode + block#
    public static final int MAX_RETRIES = 5;      // Max number of retransmissions
    public static final int SOCKET_TIMEOUT = 5000; // Socket timeout in milliseconds (5 seconds)
    
    // Error messages
    public static final String ERR_MSG_FILE_NOT_FOUND = "File not found.";
    public static final String ERR_MSG_ACCESS_VIOLATION = "Access violation.";
    public static final String ERR_MSG_DISK_FULL = "Disk full or allocation exceeded.";
    public static final String ERR_MSG_ILLEGAL_OP = "Illegal TFTP operation.";
    public static final String ERR_MSG_UNKNOWN_TID = "Unknown transfer ID.";
    public static final String ERR_MSG_FILE_EXISTS = "File already exists.";
    public static final String ERR_MSG_NO_SUCH_USER = "No such user.";
}
