package com.networkcourse.tftp.server;

import com.networkcourse.tftp.common.TFTPConstants;
import com.networkcourse.tftp.common.TFTPRequestPacket;
import com.networkcourse.tftp.util.FileTransferUtil;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a single TFTP transfer session over TCP.
 * This class manages the communication for one specific file transfer.
 */
public class TFTPSession implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(TFTPSession.class.getName()); // Add this line
    private static final int SOCKET_TIMEOUT_MS = 50000;
    private final Socket socket;
    private final String baseDirectory;
    private boolean running;
    private DatagramSocket clientSocket;
    
        public TFTPSession(Socket socket, String baseDirectory) {
            this.socket = socket;
            this.baseDirectory = baseDirectory;
            this.running = true;
        }
    
        @Override
        public void run() {
            try {
                // Set a timeout for the socket
                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
    
                try (
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream())
                ) {
                    while (true) {
                        try {
                            // Read opcode
                            short opcode = in.readShort();
    
                            if (opcode == TFTPConstants.OP_RRQ) {
                                handleReadRequest(in, out);
                            } else if (opcode == TFTPConstants.OP_WRQ) {
                                handleWriteRequest(in, out);
                            } else {
                                sendError(out, TFTPConstants.ERR_ILLEGAL_OP, TFTPConstants.ERR_MSG_ILLEGAL_OP);
                            }
                        } catch (SocketTimeoutException e) {
                            LOGGER.warning("Socket timeout: No response from client.");
                            break;
                        } catch (EOFException e) {
                            LOGGER.warning("Client closed the connection unexpectedly.");
                            break;
                        } catch (IOException e) {
                            LOGGER.warning("I/O error in session: " + e.getMessage());
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warning("Error setting socket timeout: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.warning("Error closing socket: " + e.getMessage());
                }
            }
        }
        
        /**
         * Handles a read request (client wants to download a file).
         * 
         * @param in The input stream
         * @param out The output stream
         * @throws IOException If an I/O error occurs
         */
        private void handleReadRequest(DataInputStream in, DataOutputStream out) throws IOException {
            // Read file name
            String filename = readString(in);
            
            // Read mode (we only support octet mode)
            String mode = readString(in);
            
            if (!mode.equalsIgnoreCase(TFTPConstants.MODE_OCTET)) {
                sendError(out, TFTPConstants.ERR_ILLEGAL_OP, "Only octet mode is supported");
                return;
            }
            
            LOGGER.info("Client requested file: " + filename);
            
            // Construct the file path
            String filePath = baseDirectory + File.separator + filename;
            
            // Check if the file exists and is readable
            if (!FileTransferUtil.isFileReadable(filePath)) {
                sendError(out, TFTPConstants.ERR_FILE_NOT_FOUND, TFTPConstants.ERR_MSG_FILE_NOT_FOUND);
                return;
            }
            
            // Send the file
            try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
                byte[] buffer = new byte[TFTPConstants.MAX_DATA_SIZE];
                int bytesRead;
                short blockNumber = 1;
                
                while ((bytesRead = FileTransferUtil.readBlock(fileInputStream, buffer, TFTPConstants.MAX_DATA_SIZE)) != -1) {
                    // Send data packet
                    out.writeShort(TFTPConstants.OP_DATA);
                    out.writeShort(blockNumber);
                    out.writeInt(bytesRead);  // Include length for easier processing on client side
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                    
                    blockNumber++;
                }
                
                LOGGER.info("File sent successfully: " + filename);
            }
        }
        
        /**
         * Handles a write request (client wants to upload a file).
         * 
         * @param in The input stream
         * @param out The output stream
         * @throws IOException If an I/O error occurs
         */
        private void handleWriteRequest(DataInputStream in, DataOutputStream out) throws IOException {
            // Read file name
            String filename = readString(in);
            
            // Read mode (we only support octet mode)
            String mode = readString(in);
            
            if (!mode.equalsIgnoreCase(TFTPConstants.MODE_OCTET)) {
                sendError(out, TFTPConstants.ERR_ILLEGAL_OP, "Only octet mode is supported");
                return;
            }
            
            LOGGER.info("Client wants to upload file: " + filename);
            
            // Construct the file path
            String filePath = baseDirectory + File.separator + filename;
            
            // Check if the file can be written
            if (!FileTransferUtil.isFileWritable(filePath)) {
                sendError(out, TFTPConstants.ERR_ACCESS_VIOLATION, TFTPConstants.ERR_MSG_ACCESS_VIOLATION);
                return;
            }
            
            // Check if the file already exists
            File file = new File(filePath);
            if (file.exists()) {
                sendError(out, TFTPConstants.ERR_FILE_EXISTS, TFTPConstants.ERR_MSG_FILE_EXISTS);
                return;
            }
            
            // Receive the file
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                short expectedBlock = 1;
                
                while (true) {
                    // Read opcode
                    short opcode = in.readShort();
                    
                    if (opcode != TFTPConstants.OP_DATA) {
                        sendError(out, TFTPConstants.ERR_ILLEGAL_OP, TFTPConstants.ERR_MSG_ILLEGAL_OP);
                        break;
                    }
                    
                    // Read block number and data length
                    short blockNumber = in.readShort();
                    int dataLength = in.readInt();
                    
                    if (blockNumber != expectedBlock) {
                        sendError(out, TFTPConstants.ERR_ILLEGAL_OP, "Unexpected block number");
                        break;
                    }
                    
                    // Read data
                    byte[] buffer = new byte[dataLength];
                    in.readFully(buffer, 0, dataLength);
                    
                    // Write to file
                    FileTransferUtil.writeBlock(fileOutputStream, buffer, dataLength);
                    
                    expectedBlock++;
                    
                    // If this is a partial block, it's the last one
                    if (dataLength < TFTPConstants.MAX_DATA_SIZE) {
                        break;
                    }
                }
                
                LOGGER.info("File received successfully: " + filename);
            } catch (IOException e) {
                // Delete the file if there was an error
                file.delete();
                throw e;
            }
        }
        
        /**
         * Reads a null-terminated string from the input stream.
         * 
         * @param in The input stream
         * @return The string read
         * @throws IOException If an I/O error occurs
         */
        private String readString(DataInputStream in) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != 0) {
                if (b == -1) {
                    throw new EOFException("End of stream reached while reading string");
                }
                baos.write(b);
            }
            return new String(baos.toByteArray());
        }
        
        /**
         * Sends an error packet.
         * 
         * @param out The output stream
         * @param errorCode The error code
         * @param errorMessage The error message
         * @throws IOException If an I/O error occurs
         */
        private void sendError(DataOutputStream out, short errorCode, String errorMessage) throws IOException {
            LOGGER.warning("Sending error to client: " + errorCode + " - " + errorMessage);
            
            byte[] messageBytes = errorMessage.getBytes();
            
            out.writeShort(TFTPConstants.OP_ERROR);
            out.writeShort(errorCode);
            out.writeInt(messageBytes.length);
            out.write(messageBytes);
            out.flush();
        }
        
        /**
         * Stops the session.
         */
        public void stop() throws IOException {
            running = false;
            if (!clientSocket.isClosed()) {
            clientSocket.close();
         }
    }
}
