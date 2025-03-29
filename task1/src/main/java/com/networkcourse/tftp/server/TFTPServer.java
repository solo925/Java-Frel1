package com.networkcourse.tftp.server;

import com.networkcourse.tftp.common.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TFTP Server implementation using UDP.
 * Listens for incoming TFTP requests and spawns a new session for each one.
 */
public class TFTPServer {
    private static final Logger LOGGER = Logger.getLogger(TFTPServer.class.getName());
    
    private final int port;
    private final String baseDirectory;
    private final ExecutorService executorService;
    
    private DatagramSocket mainSocket;
    private boolean running;
    private final List<TFTPSession> activeSessions;
    
    /**
     * Creates a new TFTP server.
     * 
     * @param port The port to listen on
     * @param baseDirectory The base directory for file transfers
     * @param maxConcurrentSessions The maximum number of concurrent sessions
     */
    public TFTPServer(int port, String baseDirectory, int maxConcurrentSessions) {
        this.port = port;
        this.baseDirectory = baseDirectory;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentSessions);
        this.activeSessions = new ArrayList<>();
    }
    
    /**
     * Starts the server.
     * 
     * @throws IOException If the server could not be started
     */
    public void start() throws IOException {
        if (running) {
            return;
        }
        
        // Create and bind the socket
        mainSocket = new DatagramSocket(port);
        running = true;
        
        LOGGER.info("TFTP Server started on port " + port);
        LOGGER.info("Base directory: " + baseDirectory);
        
        // Start the main listen loop
        new Thread(this::listenLoop).start();
    }
    
    /**
     * Main server loop that listens for incoming requests.
     */
    private void listenLoop() {
        byte[] buffer = new byte[TFTPConstants.MAX_PACKET_SIZE];
        
        while (running) {
            try {
                // Wait for an incoming packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mainSocket.receive(packet);
                
                // Process the packet
                processIncomingPacket(packet);
                
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Error receiving packet: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Processes an incoming packet.
     * 
     * @param packet The received packet
     */
    private void processIncomingPacket(DatagramPacket packet) {
        try {
            // Parse the packet
            TFTPPacket tftp = TFTPPacket.createFromBytes(packet.getData(), packet.getLength());
            
            // We only accept RRQ and WRQ packets on the main socket
            if (tftp.getOpcode() == TFTPConstants.OP_RRQ || tftp.getOpcode() == TFTPConstants.OP_WRQ) {
                TFTPRequestPacket requestPacket = (TFTPRequestPacket) tftp;
                
                // Only accept octet mode
                if (!TFTPConstants.MODE_OCTET.equalsIgnoreCase(requestPacket.getMode())) {
                    sendError(packet.getAddress(), packet.getPort(), 
                             TFTPConstants.ERR_ILLEGAL_OP, 
                             "Only octet mode is supported");
                    return;
                }
                
                // Create a new socket for this session
                DatagramSocket sessionSocket = new DatagramSocket();
                
                // Create client address
                InetSocketAddress clientAddress = new InetSocketAddress(
                        packet.getAddress(), packet.getPort());
                
                // Create and start a new session
                TFTPSession session = new TFTPSession(sessionSocket, clientAddress, requestPacket, baseDirectory);
                
                synchronized (activeSessions) {
                    activeSessions.add(session);
                }
                
                executorService.submit(() -> {
                    try {
                        session.run();
                    } finally {
                        synchronized (activeSessions) {
                            activeSessions.remove(session);
                        }
                    }
                });
                
            } else {
                // Unexpected packet type on main socket
                LOGGER.warning("Received unexpected packet type on main socket: " + tftp.getOpcode());
                sendError(packet.getAddress(), packet.getPort(),
                         TFTPConstants.ERR_ILLEGAL_OP,
                         "Unexpected packet type");
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error processing packet: " + e.getMessage(), e);
            
            try {
                sendError(packet.getAddress(), packet.getPort(),
                         TFTPConstants.ERR_NOT_DEFINED,
                         "Error processing request: " + e.getMessage());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error sending error packet: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Sends an error packet.
     * 
     * @param address The destination address
     * @param port The destination port
     * @param errorCode The error code
     * @param errorMessage The error message
     * @throws IOException If an I/O error occurs
     */
    private void sendError(InetAddress address, int port, short errorCode, String errorMessage) throws IOException {
        TFTPErrorPacket errorPacket = new TFTPErrorPacket(errorCode, errorMessage);
        byte[] serializedError = errorPacket.serialize();
        
        DatagramPacket outPacket = new DatagramPacket(
                serializedError, 
                serializedError.length,
                address,
                port);
        
        mainSocket.send(outPacket);
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Stop all active sessions
        synchronized (activeSessions) {
            for (TFTPSession session : activeSessions) {
                session.stop();
            }
            activeSessions.clear();
        }
        
        // Close the main socket
        if (mainSocket != null) {
            mainSocket.close();
            mainSocket = null;
        }
        
        // Shutdown the executor service
        executorService.shutdownNow();
        
        LOGGER.info("TFTP Server stopped");
    }
    
    /**
     * Main method to start the server.
     * 
     * @param args Command line arguments (optional: port)
     */
    public static void main(String[] args) {
        int port = TFTPConstants.DEFAULT_PORT;
        
        // Parse port number from command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        // Use current directory as base directory
        String baseDirectory = System.getProperty("user.dir");
        
        // Create and start the server
        TFTPServer server = new TFTPServer(port, baseDirectory, 10);
        
        try {
            server.start();
            
            // Add shutdown hook to stop the server cleanly
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            System.out.println("TFTP Server started on port " + port);
            System.out.println("Base directory: " + baseDirectory);
            System.out.println("Press Ctrl+C to stop the server");
            
            // Wait for the server to stop
            while (server.running) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
