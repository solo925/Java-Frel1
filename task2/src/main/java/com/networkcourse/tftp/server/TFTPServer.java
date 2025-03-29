package com.networkcourse.tftp.server;

import com.networkcourse.tftp.common.TFTPConstants;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TFTP Server implementation using TCP.
 * Listens for incoming TCP connections and spawns a new session for each one.
 */
public class TFTPServer {
    private static final Logger LOGGER = Logger.getLogger(TFTPServer.class.getName());
    
    private final int port;
    private final String baseDirectory;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private boolean running;
    
    /**
     * Creates a new TFTP server.
     * 
     * @param port The port to listen on
     * @param baseDirectory The base directory for file operations
     * @param maxConcurrentSessions The maximum number of concurrent sessions
     */
    public TFTPServer(int port, String baseDirectory, int maxConcurrentSessions) {
        this.port = port;
        this.baseDirectory = baseDirectory;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentSessions);
    }
    
    /**
     * Starts the server.
     * 
     * @throws IOException If the server could not be started
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        
        // Start the main server loop in a new thread
        new Thread(this::listenLoop).start();
    }
    
    /**
     * Main server loop that listens for incoming connections.
     */
    private void listenLoop() {
        LOGGER.info("Server started on port " + port);
        LOGGER.info("Base directory: " + baseDirectory);
        
        while (running) {
            try {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();
                
                // Configure socket timeout
                clientSocket.setSoTimeout(5000); // 5 seconds
                
                LOGGER.info("New client connected: " + clientSocket.getInetAddress().getHostAddress() + 
                           ":" + clientSocket.getPort());
                
                // Create and start a new session for this client
                TFTPSession session = new TFTPSession(clientSocket, baseDirectory);
                executorService.submit(session);
                
            } catch (SocketException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Socket error in server listen loop", e);
                }
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "I/O error in server listen loop", e);
                }
            }
        }
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        running = false;
        
        // Close the server socket to interrupt the accept() call
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing server socket", e);
        }
        
        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        
        LOGGER.info("Server stopped");
    }
    
    /**
     * Main method to start the server.
     * 
     * @param args Command line arguments (optional: port)
     */
    public static void main(String[] args) {
        int port = TFTPConstants.DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
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
