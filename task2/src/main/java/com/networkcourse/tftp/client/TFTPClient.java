package com.networkcourse.tftp.client;

import com.networkcourse.tftp.common.TFTPConstants;
import com.networkcourse.tftp.util.FileTransferUtil;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TFTP Client implementation using TCP.
 * Allows users to upload and download files from a TFTP server.
 */
public class TFTPClient {
    private static final Logger LOGGER = Logger.getLogger(TFTPClient.class.getName());
    
    private final String serverHost;
    private final int serverPort;
    private final Scanner scanner;
    
    /**
     * Creates a new TFTP client.
     * 
     * @param serverHost The server hostname or IP address
     * @param serverPort The server port
     */
    public TFTPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Starts the client's interactive console.
     */
    public void start() {
        System.out.println("TFTP Client (TCP) started");
        System.out.println("Server: " + serverHost + ":" + serverPort);
        
        boolean running = true;
        
        while (running) {
            System.out.println("\nCommands:");
            System.out.println("1. Download file (GET)");
            System.out.println("2. Upload file (PUT)");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    downloadFile();
                    break;
                case "2":
                    uploadFile();
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }
        
        System.out.println("TFTP Client exiting...");
    }
    
    /**
     * Handles downloading a file from the server.
     */
    private void downloadFile() {
        try {
            System.out.print("Enter remote filename: ");
            String remoteFile = scanner.nextLine().trim();
            
            System.out.print("Enter local filename (default: same as remote): ");
            String localFile = scanner.nextLine().trim();
            
            if (localFile.isEmpty()) {
                localFile = remoteFile;
            }
            
            // Check if the local file can be written
            if (!FileTransferUtil.isFileWritable(localFile)) {
                System.out.println("Error: Cannot write to local file " + localFile);
                return;
            }
            
            System.out.println("Downloading " + remoteFile + " to " + localFile + "...");
            
            // Connect to the server
            try (
                Socket socket = new Socket(serverHost, serverPort);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(localFile)
            ) {
                // Send read request
                out.writeShort(TFTPConstants.OP_RRQ);
                writeString(out, remoteFile);
                writeString(out, TFTPConstants.MODE_OCTET);
                out.flush();
                
                boolean fileComplete = false;
                int totalBytes = 0;
                
                // Receive data packets until the file is complete
                while (!fileComplete) {
                    try {
                        // Read opcode
                        short opcode = in.readShort();
                        
                        if (opcode == TFTPConstants.OP_DATA) {
                            // Read block number and data length
                            short blockNumber = in.readShort();
                            int dataLength = in.readInt();
                            
                            // Read data
                            byte[] buffer = new byte[dataLength];
                            in.readFully(buffer, 0, dataLength);
                            
                            // Write to file
                            FileTransferUtil.writeBlock(fileOutputStream, buffer, dataLength);
                            
                            totalBytes += dataLength;
                            
                            // Update progress
                            System.out.print("\rReceived " + totalBytes + " bytes");
                            
                            // If this is a partial block, it's the last one
                            if (dataLength < TFTPConstants.MAX_DATA_SIZE) {
                                fileComplete = true;
                            }
                            
                        } else if (opcode == TFTPConstants.OP_ERROR) {
                            // Read error code and message
                            short errorCode = in.readShort();
                            int messageLength = in.readInt();
                            byte[] messageBytes = new byte[messageLength];
                            in.readFully(messageBytes, 0, messageLength);
                            String errorMessage = new String(messageBytes);
                            
                            System.out.println("\nError from server: " + errorCode + " - " + errorMessage);
                            return;
                            
                        } else {
                            System.out.println("\nUnexpected response from server");
                            return;
                        }
                    } catch (EOFException e) {
                        System.out.println("\nError: Server closed the connection unexpectedly.");
                        return;
                    }
                }
                
                System.out.println("\nDownload complete. " + totalBytes + " bytes received.");
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                LOGGER.log(Level.WARNING, "Error downloading file", e);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Error downloading file", e);
        }
    }
    
    /**
     * Handles uploading a file to the server.
     */
    private void uploadFile() {
        try {
            System.out.print("Enter local filename: ");
            String localFile = scanner.nextLine().trim();
            
            // Check if the local file exists and is readable
            if (!FileTransferUtil.isFileReadable(localFile)) {
                System.out.println("Error: Cannot read local file " + localFile);
                return;
            }
            
            System.out.print("Enter remote filename (default: same as local): ");
            String remoteFile = scanner.nextLine().trim();
            
            if (remoteFile.isEmpty()) {
                remoteFile = localFile;
            }
            
            System.out.println("Uploading " + localFile + " to " + remoteFile + "...");
            
            // Connect to the server
            try (
                Socket socket = new Socket(serverHost, serverPort);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());
                FileInputStream fileInputStream = new FileInputStream(localFile)
            ) {
                // Send write request
                out.writeShort(TFTPConstants.OP_WRQ);
                writeString(out, remoteFile);
                writeString(out, TFTPConstants.MODE_OCTET);
                out.flush();
                
                // Check for error response
                short opcode = in.readShort();
                if (opcode == TFTPConstants.OP_ERROR) {
                    // Read error code and message
                    short errorCode = in.readShort();
                    int messageLength = in.readInt();
                    byte[] messageBytes = new byte[messageLength];
                    in.readFully(messageBytes, 0, messageLength);
                    String errorMessage = new String(messageBytes);
                    
                    System.out.println("Error from server: " + errorCode + " - " + errorMessage);
                    return;
                }
                
                // Send file data
                byte[] buffer = new byte[TFTPConstants.MAX_DATA_SIZE];
                int bytesRead;
                short blockNumber = 1;
                int totalBytes = 0;
                
                while ((bytesRead = FileTransferUtil.readBlock(fileInputStream, buffer, TFTPConstants.MAX_DATA_SIZE)) != -1) {
                    // Send data packet
                    out.writeShort(TFTPConstants.OP_DATA);
                    out.writeShort(blockNumber);
                    out.writeInt(bytesRead);
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                    
                    totalBytes += bytesRead;
                    blockNumber++;
                    
                    // Update progress
                    System.out.print("\rSent " + totalBytes + " bytes");
                }
                
                System.out.println("\nUpload complete. " + totalBytes + " bytes sent.");
            } catch (java.net.ConnectException e) {
                System.out.println("Error: Could not connect to the server. Ensure the server is running and the port is correct.");
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                LOGGER.log(Level.WARNING, "Error uploading file", e);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Error uploading file", e);
        }
    }
    
    /**
     * Writes a null-terminated string to the output stream.
     * 
     * @param out The output stream
     * @param str The string to write
     * @throws IOException If an I/O error occurs
     */
    private void writeString(DataOutputStream out, String str) throws IOException {
        out.write(str.getBytes());
        out.write(0);  // Null terminator
    }
    
    /**
     * Main method to start the client.
     * 
     * @param args Command line arguments (optional: server_host, server_port)
     */
    public static void main(String[] args) {
        String serverHost = "localhost";
        int serverPort = TFTPConstants.DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            serverHost = args[0];
        }
        
        if (args.length > 1) {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }

        try (Socket socket = new Socket(serverHost, serverPort)) {
            System.out.println("Connected to server at " + serverHost + ":" + serverPort);
        } catch (IOException e) {
            System.err.println("Error: Could not connect to the server. Ensure the server is running and the port is correct.");
            System.exit(1);
        }
        
        // Create and start the client
        TFTPClient client = new TFTPClient(serverHost, serverPort);
        client.start();
    }
}
