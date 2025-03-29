package com.networkcourse.tftp.server;

import com.networkcourse.tftp.common.*;
import com.networkcourse.tftp.util.FileTransferUtil;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a single TFTP transfer session over UDP.
 * This class manages the communication for one specific file transfer.
 */
public class TFTPSession implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TFTPSession.class.getName());
    
    private final DatagramSocket socket;
    private final InetSocketAddress clientAddress;
    private final TFTPRequestPacket requestPacket;
    private final String baseDirectory;
    
    private boolean running = true;
    
    /**
     * Creates a new TFTP session.
     * 
     * @param socket The socket to use for communication
     * @param clientAddress The address of the client
     * @param requestPacket The initial request packet
     * @param baseDirectory The base directory for file operations
     */
    public TFTPSession(DatagramSocket socket, InetSocketAddress clientAddress, 
                        TFTPRequestPacket requestPacket, String baseDirectory) {
        this.socket = socket;
        this.clientAddress = clientAddress;
        this.requestPacket = requestPacket;
        this.baseDirectory = baseDirectory;
    }
    
    @Override
    public void run() {
        try {
            // Set timeout for socket
            socket.setSoTimeout(TFTPConstants.SOCKET_TIMEOUT);
            
            // Handle the request based on its type
            if (requestPacket.isReadRequest()) {
                handleReadRequest();
            } else if (requestPacket.isWriteRequest()) {
                handleWriteRequest();
            } else {
                LOGGER.warning("Unsupported request type: " + requestPacket.getOpcode());
                sendError(TFTPConstants.ERR_ILLEGAL_OP, TFTPConstants.ERR_MSG_ILLEGAL_OP);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IO error in session", e);
        } finally {
            socket.close();
        }
    }
    
    /**
     * Handles a read request (client wants to download a file).
     */
    private void handleReadRequest() {
        LOGGER.info("Handling read request for file: " + requestPacket.getFilename());
        
        String filePath = baseDirectory + File.separator + requestPacket.getFilename();
        try (FileInputStream fileInputStream = FileTransferUtil.openFileForReading(filePath)) {
            // Check if file exists and is readable
            if (!FileTransferUtil.isFileReadable(filePath)) {
                sendError(TFTPConstants.ERR_FILE_NOT_FOUND, TFTPConstants.ERR_MSG_FILE_NOT_FOUND);
                return;
            }
            
            byte[] buffer = new byte[TFTPConstants.MAX_DATA_SIZE];
            int bytesRead;
            short blockNumber = 1;
            
            // Read and send blocks until the file is completely sent
            do {
                // Read a block from the file
                bytesRead = FileTransferUtil.readBlock(fileInputStream, buffer, TFTPConstants.MAX_DATA_SIZE);
                
                if (bytesRead >= 0) {
                    // Create and send data packet
                    TFTPDataPacket dataPacket = new TFTPDataPacket(blockNumber, buffer, bytesRead);
                    boolean success = sendDataAndWaitForAck(dataPacket, blockNumber);
                    
                    if (!success) {
                        LOGGER.warning("Failed to send data block " + blockNumber);
                        return;
                    }
                    
                    blockNumber++;
                }
            } while (bytesRead == TFTPConstants.MAX_DATA_SIZE && running);
            
            LOGGER.info("File " + requestPacket.getFilename() + " sent successfully");
            
        } catch (FileNotFoundException e) {
            sendError(TFTPConstants.ERR_FILE_NOT_FOUND, "File not found: " + requestPacket.getFilename());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading file: " + requestPacket.getFilename(), e);
            sendError(TFTPConstants.ERR_NOT_DEFINED, "Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Sends a data packet and waits for an acknowledgment.
     * Implements the retransmission logic.
     * 
     * @param dataPacket The data packet to send
     * @param blockNumber The block number to expect in the ACK
     * @return true if the data was acknowledged, false otherwise
     */
    private boolean sendDataAndWaitForAck(TFTPDataPacket dataPacket, short blockNumber) throws IOException {
        byte[] serializedData = dataPacket.serialize();
        DatagramPacket outPacket = new DatagramPacket(
                serializedData, 
                serializedData.length, 
                clientAddress.getAddress(), 
                clientAddress.getPort());
        
        for (int retry = 0; retry < TFTPConstants.MAX_RETRIES; retry++) {
            // Send the data packet
            socket.send(outPacket);
            
            // Wait for ACK
            try {
                byte[] receiveBuffer = new byte[TFTPConstants.MAX_PACKET_SIZE];
                DatagramPacket inPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                
                socket.receive(inPacket);
                
                // Parse the received packet
                TFTPPacket receivedPacket = TFTPPacket.createFromBytes(inPacket.getData(), inPacket.getLength());
                
                if (receivedPacket.getOpcode() == TFTPConstants.OP_ACK) {
                    TFTPAckPacket ackPacket = (TFTPAckPacket) receivedPacket;
                    
                    if (ackPacket.getBlockNumber() == blockNumber) {
                        // Successfully acknowledged
                        return true;
                    } else {
                        LOGGER.warning("Received ACK for wrong block, expected " + 
                                       blockNumber + " but got " + ackPacket.getBlockNumber());
                    }
                }
                
            } catch (SocketTimeoutException e) {
                LOGGER.info("Timeout waiting for ACK, retrying (" + (retry + 1) + 
                           "/" + TFTPConstants.MAX_RETRIES + ")");
            }
        }
        
        // If we get here, we've run out of retries
        return false;
    }
    
    /**
     * Handles a write request (client wants to upload a file).
     */
    private void handleWriteRequest() {
        LOGGER.info("Handling write request for file: " + requestPacket.getFilename());
        
        String filePath = baseDirectory + File.separator + requestPacket.getFilename();
        try (FileOutputStream fileOutputStream = FileTransferUtil.openFileForWriting(filePath)) {
            // Check if file can be written
            if (!FileTransferUtil.isFileWritable(filePath)) {
                sendError(TFTPConstants.ERR_ACCESS_VIOLATION, TFTPConstants.ERR_MSG_ACCESS_VIOLATION);
                return;
            }
            
            File targetFile = new File(filePath);
            if (targetFile.exists()) {
                sendError(TFTPConstants.ERR_FILE_EXISTS, TFTPConstants.ERR_MSG_FILE_EXISTS);
                return;
            }
            
            // Send initial ACK with block number 0 to indicate we're ready to receive data
            sendAck((short) 0);
            
            short expectedBlock = 1;
            boolean lastPacket = false;
            
            // Receive data packets until we get a packet smaller than the maximum size or an error occurs
            while (!lastPacket && running) {
                // Receive a data packet
                TFTPDataPacket dataPacket = receiveData(expectedBlock);
                
                if (dataPacket == null) {
                    // Failed to receive expected data
                    LOGGER.warning("Failed to receive data block " + expectedBlock);
                    return;
                }
                
                // Write the data to the file
                FileTransferUtil.writeBlock(fileOutputStream, dataPacket.getData(), dataPacket.getDataLength());
                
                // Send ACK
                sendAck(expectedBlock);
                
                // Check if this is the last packet
                if (dataPacket.getDataLength() < TFTPConstants.MAX_DATA_SIZE) {
                    lastPacket = true;
                }
                
                expectedBlock++;
            }
            
            LOGGER.info("File " + requestPacket.getFilename() + " received successfully");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing file: " + requestPacket.getFilename(), e);
            sendError(TFTPConstants.ERR_NOT_DEFINED, "Error writing file: " + e.getMessage());
            
            // Try to delete the incomplete file
            new File(filePath).delete();
        }
    }
    
    /**
     * Receives a data packet from the client.
     * 
     * @param expectedBlock The expected block number
     * @return The received data packet, or null if an error occurred
     */
    private TFTPDataPacket receiveData(short expectedBlock) throws IOException {
        byte[] receiveBuffer = new byte[TFTPConstants.MAX_PACKET_SIZE];
        DatagramPacket inPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        
        for (int retry = 0; retry < TFTPConstants.MAX_RETRIES; retry++) {
            try {
                socket.receive(inPacket);
                
                // Verify packet is from the correct client
                if (!inPacket.getAddress().equals(clientAddress.getAddress()) || 
                    inPacket.getPort() != clientAddress.getPort()) {
                    // Packet from unknown source
                    sendError(inPacket.getAddress(), inPacket.getPort(), 
                             TFTPConstants.ERR_UNKNOWN_TID, TFTPConstants.ERR_MSG_UNKNOWN_TID);
                    continue;
                }
                
                // Parse the received packet
                TFTPPacket receivedPacket = TFTPPacket.createFromBytes(inPacket.getData(), inPacket.getLength());
                
                if (receivedPacket.getOpcode() == TFTPConstants.OP_DATA) {
                    TFTPDataPacket dataPacket = (TFTPDataPacket) receivedPacket;
                    
                    if (dataPacket.getBlockNumber() == expectedBlock) {
                        // We got the expected data packet
                        return dataPacket;
                    } else {
                        LOGGER.warning("Received data packet with wrong block number, expected " + 
                                      expectedBlock + " but got " + dataPacket.getBlockNumber());
                        // Send ACK for the previous block to trigger retransmission
                        sendAck((short) (expectedBlock - 1));
                    }
                } else if (receivedPacket.getOpcode() == TFTPConstants.OP_ERROR) {
                    TFTPErrorPacket errorPacket = (TFTPErrorPacket) receivedPacket;
                    LOGGER.warning("Received error from client: " + errorPacket.getErrorCode() + 
                                  " - " + errorPacket.getErrorMessage());
                    return null;
                }
                
            } catch (SocketTimeoutException e) {
                LOGGER.info("Timeout waiting for data block " + expectedBlock + 
                           ", retrying (" + (retry + 1) + "/" + TFTPConstants.MAX_RETRIES + ")");
                // Resend the ACK for the previous block (or the initial ACK)
                sendAck((short) (expectedBlock - 1));
            }
        }
        
        // If we get here, we've run out of retries
        return null;
    }
    
    /**
     * Sends an acknowledgment packet.
     * 
     * @param blockNumber The block number to acknowledge
     */
    private void sendAck(short blockNumber) throws IOException {
        TFTPAckPacket ackPacket = new TFTPAckPacket(blockNumber);
        byte[] serializedAck = ackPacket.serialize();
        
        DatagramPacket outPacket = new DatagramPacket(
                serializedAck, 
                serializedAck.length,
                clientAddress.getAddress(),
                clientAddress.getPort());
        
        socket.send(outPacket);
    }
    
    /**
     * Sends an error packet to the client.
     * 
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    private void sendError(short errorCode, String errorMessage) {
        try {
            TFTPErrorPacket errorPacket = new TFTPErrorPacket(errorCode, errorMessage);
            byte[] serializedError = errorPacket.serialize();
            
            DatagramPacket outPacket = new DatagramPacket(
                    serializedError,
                    serializedError.length,
                    clientAddress.getAddress(),
                    clientAddress.getPort());
            
            socket.send(outPacket);
            
            LOGGER.warning("Sent error to " + clientAddress.getAddress().getHostAddress() + 
                          ":" + clientAddress.getPort() + " - " + errorCode + " - " + errorMessage);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send error packet", e);
        }
    }
    
    /**
     * Sends an error packet to a specific address and port.
     * 
     * @param address The destination address
     * @param port The destination port
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    private void sendError(java.net.InetAddress address, int port, short errorCode, String errorMessage) {
        try {
            TFTPErrorPacket errorPacket = new TFTPErrorPacket(errorCode, errorMessage);
            byte[] serializedError = errorPacket.serialize();
            
            DatagramPacket outPacket = new DatagramPacket(
                    serializedError,
                    serializedError.length,
                    address,
                    port);
            
            socket.send(outPacket);
            
            LOGGER.warning("Sent error to " + address.getHostAddress() + 
                          ":" + port + " - " + errorCode + " - " + errorMessage);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send error packet", e);
        }
    }
    
    /**
     * Stops the session.
     */
    public void stop() {
        running = false;
    }
}
