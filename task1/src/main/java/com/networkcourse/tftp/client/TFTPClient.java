package com.networkcourse.tftp.client;

import com.networkcourse.tftp.common.*;
import com.networkcourse.tftp.util.FileTransferUtil;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TFTP Client implementation using UDP.
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
        System.out.println("TFTP Client started");
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
            
            // Create a datagram socket
            try (DatagramSocket socket = new DatagramSocket()) {
                // Set timeout
                socket.setSoTimeout(TFTPConstants.SOCKET_TIMEOUT);
                
                // Create and send read request
                TFTPRequestPacket rrqPacket = new TFTPRequestPacket(
                        TFTPConstants.OP_RRQ, remoteFile, TFTPConstants.MODE_OCTET);
                
                byte[] rrqData = rrqPacket.serialize();
                
                InetAddress serverAddress = InetAddress.getByName(serverHost);
                DatagramPacket outPacket = new DatagramPacket(
                        rrqData, rrqData.length, serverAddress, serverPort);
                
                socket.send(outPacket);
                
                // Open output file
                try (FileOutputStream fileOutputStream = new FileOutputStream(localFile)) {
                    short expectedBlock = 1;
                    boolean lastPacket = false;
                    int totalBytes = 0;
                    
                    // Receive data packets until the file is completely received
                    while (!lastPacket) {
                        TFTPDataPacket dataPacket = receiveData(socket, expectedBlock);
                        
                        if (dataPacket == null) {
                            System.out.println("Error: File transfer failed.");
                            return;
                        }
                        
                        // Write the data to the file
                        FileTransferUtil.writeBlock(fileOutputStream, 
                                dataPacket.getData(), dataPacket.getDataLength());
                        
                        totalBytes += dataPacket.getDataLength();
                        
                        // Send ACK for the block
                        sendAck(socket, serverAddress, 
                               ((InetSocketAddress) outPacket.getSocketAddress()).getPort(), 
                               expectedBlock);
                        
                        // Check if this is the last packet
                        lastPacket = dataPacket.isFinalPacket();
                        expectedBlock++;
                        
                        // Update progress
                        System.out.print("\rReceived " + totalBytes + " bytes");
                    }
                    
                    System.out.println("\nDownload complete. " + totalBytes + " bytes received.");
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Error downloading file", e);
        }
    }
    
    /**
     * Receives a data packet from the server.
     * 
     * @param socket The socket to receive on
     * @param expectedBlock The expected block number
     * @return The received data packet, or null if it could not be received
     * @throws IOException If an I/O error occurs
     */
    private TFTPDataPacket receiveData(DatagramSocket socket, short expectedBlock) throws IOException {
        byte[] buffer = new byte[TFTPConstants.MAX_PACKET_SIZE];
        DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
        
        for (int retry = 0; retry < TFTPConstants.MAX_RETRIES; retry++) {
            try {
                socket.receive(inPacket);
                
                // Parse the received packet
                TFTPPacket receivedPacket = TFTPPacket.createFromBytes(inPacket.getData(), inPacket.getLength());
                
                if (receivedPacket.getOpcode() == TFTPConstants.OP_DATA) {
                    TFTPDataPacket dataPacket = (TFTPDataPacket) receivedPacket;
                    
                    if (dataPacket.getBlockNumber() == expectedBlock) {
                        // Got the expected data packet
                        return dataPacket;
                    } else {
                        // Got a data packet with unexpected block number
                        LOGGER.warning("Received data packet with wrong block number, expected " + 
                                      expectedBlock + ", got " + dataPacket.getBlockNumber());
                    }
                } else if (receivedPacket.getOpcode() == TFTPConstants.OP_ERROR) {
                    // Server sent an error
                    TFTPErrorPacket errorPacket = (TFTPErrorPacket) receivedPacket;
                    System.out.println("Error from server: " + 
                                     errorPacket.getErrorCode() + " - " + 
                                     errorPacket.getErrorMessage());
                    return null;
                }
                
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout waiting for data, retrying (" + (retry + 1) + "/" + 
                                 TFTPConstants.MAX_RETRIES + ")");
            }
        }
        
        // If we got here, we ran out of retries
        System.out.println("Maximum retries reached, giving up");
        return null;
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
            
            // Create a datagram socket
            try (DatagramSocket socket = new DatagramSocket()) {
                // Set timeout
                socket.setSoTimeout(TFTPConstants.SOCKET_TIMEOUT);
                
                // Create and send write request
                TFTPRequestPacket wrqPacket = new TFTPRequestPacket(
                        TFTPConstants.OP_WRQ, remoteFile, TFTPConstants.MODE_OCTET);
                
                byte[] wrqData = wrqPacket.serialize();
                
                InetAddress serverAddress = InetAddress.getByName(serverHost);
                DatagramPacket outPacket = new DatagramPacket(
                        wrqData, wrqData.length, serverAddress, serverPort);
                
                socket.send(outPacket);
                
                // Wait for initial ACK
                TFTPAckPacket ackPacket = receiveAck(socket, (short) 0);
                
                if (ackPacket == null) {
                    System.out.println("Error: Failed to receive initial acknowledgment.");
                    return;
                }
                
                // Remember server's TID (port)
                int serverTID = ((InetSocketAddress) outPacket.getSocketAddress()).getPort();
                
                // Open input file
                try (FileInputStream fileInputStream = new FileInputStream(localFile)) {
                    byte[] buffer = new byte[TFTPConstants.MAX_DATA_SIZE];
                    short blockNumber = 1;
                    int bytesRead;
                    int totalBytes = 0;
                    
                    // Read and send blocks until the file is completely sent
                    do {
                        // Read a block from the file
                        bytesRead = FileTransferUtil.readBlock(fileInputStream, buffer, TFTPConstants.MAX_DATA_SIZE);
                        
                        if (bytesRead >= 0) {
                            // Create and send data packet
                            TFTPDataPacket dataPacket = new TFTPDataPacket(blockNumber, buffer, bytesRead);
                            boolean success = sendDataAndWaitForAck(socket, serverAddress, serverTID, 
                                                                  dataPacket, blockNumber);
                            
                            if (!success) {
                                System.out.println("Error: Failed to send data block " + blockNumber);
                                return;
                            }
                            
                            totalBytes += bytesRead;
                            blockNumber++;
                            
                            // Update progress
                            System.out.print("\rSent " + totalBytes + " bytes");
                        }
                    } while (bytesRead == TFTPConstants.MAX_DATA_SIZE);
                    
                    System.out.println("\nUpload complete. " + totalBytes + " bytes sent.");
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Error uploading file", e);
        }
    }
    
    /**
     * Sends a data packet and waits for an acknowledgment.
     * 
     * @param socket The socket to use
     * @param serverAddress The server address
     * @param serverPort The server port
     * @param dataPacket The data packet to send
     * @param expectedBlock The expected block number for the ACK
     * @return true if the data was acknowledged, false otherwise
     * @throws IOException If an I/O error occurs
     */
    private boolean sendDataAndWaitForAck(DatagramSocket socket, InetAddress serverAddress, 
                                         int serverPort, TFTPDataPacket dataPacket, 
                                         short expectedBlock) throws IOException {
        byte[] serializedData = dataPacket.serialize();
        DatagramPacket outPacket = new DatagramPacket(
                serializedData, 
                serializedData.length,
                serverAddress,
                serverPort);
        
        for (int retry = 0; retry < TFTPConstants.MAX_RETRIES; retry++) {
            // Send the data packet
            socket.send(outPacket);
            
            // Wait for ACK
            TFTPAckPacket ackPacket = receiveAck(socket, expectedBlock);
            
            if (ackPacket != null) {
                return true;
            }
            
            System.out.println("Retrying send of block " + expectedBlock + 
                             " (" + (retry + 1) + "/" + TFTPConstants.MAX_RETRIES + ")");
        }
        
        // If we got here, we ran out of retries
        System.out.println("Maximum retries reached, giving up");
        return false;
    }
    
    /**
     * Receives an acknowledgment packet from the server.
     * 
     * @param socket The socket to receive on
     * @param expectedBlock The expected block number
     * @return The received ACK packet, or null if it could not be received
     * @throws IOException If an I/O error occurs
     */
    private TFTPAckPacket receiveAck(DatagramSocket socket, short expectedBlock) throws IOException {
        byte[] buffer = new byte[TFTPConstants.MAX_PACKET_SIZE];
        DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
        
        try {
            socket.receive(inPacket);
            
            // Parse the received packet
            TFTPPacket receivedPacket = TFTPPacket.createFromBytes(inPacket.getData(), inPacket.getLength());
            
            if (receivedPacket.getOpcode() == TFTPConstants.OP_ACK) {
                TFTPAckPacket ackPacket = (TFTPAckPacket) receivedPacket;
                
                if (ackPacket.getBlockNumber() == expectedBlock) {
                    // Got the expected ACK
                    return ackPacket;
                } else {
                    // Got an ACK with unexpected block number
                    LOGGER.warning("Received ACK with wrong block number, expected " + 
                                  expectedBlock + ", got " + ackPacket.getBlockNumber());
                }
            } else if (receivedPacket.getOpcode() == TFTPConstants.OP_ERROR) {
                // Server sent an error
                TFTPErrorPacket errorPacket = (TFTPErrorPacket) receivedPacket;
                System.out.println("Error from server: " + 
                                 errorPacket.getErrorCode() + " - " + 
                                 errorPacket.getErrorMessage());
                return null;
            }
            
        } catch (SocketTimeoutException e) {
            LOGGER.warning("Timeout waiting for ACK");
        }
        
        return null;
    }
    
    /**
     * Sends an acknowledgment packet.
     * 
     * @param socket The socket to send on
     * @param address The destination address
     * @param port The destination port
     * @param blockNumber The block number to acknowledge
     * @throws IOException If an I/O error occurs
     */
    private void sendAck(DatagramSocket socket, InetAddress address, 
                        int port, short blockNumber) throws IOException {
        TFTPAckPacket ackPacket = new TFTPAckPacket(blockNumber);
        byte[] serializedAck = ackPacket.serialize();
        
        DatagramPacket outPacket = new DatagramPacket(
                serializedAck, 
                serializedAck.length,
                address,
                port);
        
        socket.send(outPacket);
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
        
        // Create and start the client
        TFTPClient client = new TFTPClient(serverHost, serverPort);
        client.start();
    }
}
