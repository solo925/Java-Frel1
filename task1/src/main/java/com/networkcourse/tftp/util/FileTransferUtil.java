package com.networkcourse.tftp.util;

import java.io.*;
import java.util.Arrays;

/**
 * Utility class for file transfer operations.
 * Handles reading and writing files in binary mode for TFTP protocol.
 */
public class FileTransferUtil {
    
    /**
     * Reads a block of data from a file.
     * 
     * @param fileInputStream The input stream to read from
     * @param buffer The buffer to read into
     * @param blockSize The maximum block size to read
     * @return The number of bytes read, or -1 if end of file
     * @throws IOException If an I/O error occurs
     */
    public static int readBlock(InputStream fileInputStream, byte[] buffer, int blockSize) throws IOException {
        return fileInputStream.read(buffer, 0, blockSize);
    }
    
    /**
     * Writes a block of data to a file.
     * 
     * @param fileOutputStream The output stream to write to
     * @param data The data to write
     * @param length The length of data to write
     * @throws IOException If an I/O error occurs
     */
    public static void writeBlock(OutputStream fileOutputStream, byte[] data, int length) throws IOException {
        fileOutputStream.write(data, 0, length);
    }
    
    /**
     * Creates a new file input stream for reading.
     * 
     * @param filename The name of the file to open
     * @return A FileInputStream for the specified file
     * @throws FileNotFoundException If the file does not exist
     */
    public static FileInputStream openFileForReading(String filename) throws FileNotFoundException {
        return new FileInputStream(filename);
    }
    
    /**
     * Creates a new file output stream for writing.
     * 
     * @param filename The name of the file to open
     * @return A FileOutputStream for the specified file
     * @throws IOException If an I/O error occurs
     */
    public static FileOutputStream openFileForWriting(String filename) throws IOException {
        return new FileOutputStream(filename);
    }
    
    /**
     * Checks if a file exists and is readable.
     * 
     * @param filename The name of the file to check
     * @return true if the file exists and is readable, false otherwise
     */
    public static boolean isFileReadable(String filename) {
        File file = new File(filename);
        return file.exists() && file.isFile() && file.canRead();
    }
    
    /**
     * Checks if a file can be created and written to.
     * 
     * @param filename The name of the file to check
     * @return true if the file can be created and written to, false otherwise
     */
    public static boolean isFileWritable(String filename) {
        File file = new File(filename);
        
        if (file.exists()) {
            return file.isFile() && file.canWrite();
        } else {
            // Check if the parent directory exists and is writable
            File parentDir = file.getParentFile();
            if (parentDir == null) {
                parentDir = new File(".");
            }
            return parentDir.exists() && parentDir.isDirectory() && parentDir.canWrite();
        }
    }
    
    /**
     * Safely closes a closeable resource.
     * 
     * @param closeable The resource to close
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
