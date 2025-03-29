package com.networkcourse.tftp.util;

import java.io.*;

/**
 * Utility class for file operations used in the TFTP implementation.
 */
public class FileTransferUtil {
    
    /**
     * Checks if a file exists and is readable.
     * 
     * @param filePath Path to the file
     * @return true if the file exists and is readable, false otherwise
     */
    public static boolean isFileReadable(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canRead();
    }
    
    /**
     * Checks if a file can be written to.
     * If the file doesn't exist, checks if the parent directory is writable.
     * 
     * @param filePath Path to the file
     * @return true if the file can be written to, false otherwise
     */
    public static boolean isFileWritable(String filePath) {
        File file = new File(filePath);
        
        if (file.exists()) {
            return file.canWrite();
        } else {
            // Check if parent directory exists and is writable
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                return parentDir.exists() && parentDir.canWrite();
            } else {
                // File is in the current directory
                return new File(".").canWrite();
            }
        }
    }
    
    /**
     * Reads a block of data from a file input stream.
     * 
     * @param fis The file input stream
     * @param buffer The buffer to read into
     * @param maxLength The maximum number of bytes to read
     * @return The number of bytes read, or -1 if end of file
     * @throws IOException If an I/O error occurs
     */
    public static int readBlock(InputStream fis, byte[] buffer, int maxLength) throws IOException {
        return fis.read(buffer, 0, maxLength);
    }
    
    /**
     * Writes a block of data to a file output stream.
     * 
     * @param fos The file output stream
     * @param buffer The buffer to write from
     * @param length The number of bytes to write
     * @throws IOException If an I/O error occurs
     */
    public static void writeBlock(OutputStream fos, byte[] buffer, int length) throws IOException {
        fos.write(buffer, 0, length);
    }
}
