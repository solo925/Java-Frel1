# TFTP Implementation in Java (Task 1)

This project implements the Trivial File Transfer Protocol (TFTP) as specified in RFC 1350. It focuses on Task 1 of the Computer Networks assignment.

## Features
- Complete UDP-based TFTP implementation following RFC 1350
- Support for octet mode only (raw byte transfers)
- Error handling for file not found
- Console-based client and server applications
- Support for simultaneous file transfers with multiple clients

## How to Run

### 1. Compile the Project
Run the following command in the `task1` directory:
```bash
javac -d target/classes src/main/java/com/networkcourse/tftp/**/*.java
```

### 2. Start the TFTP Server
Run the server:
```bash
java -cp target/classes com.networkcourse.tftp.server.TFTPServer
```

### 3. Start the TFTP Client
Run the client:
```bash
java -cp target/classes com.networkcourse.tftp.client.TFTPClient localhost 6969
```

### 4. Use the Client
Once the client starts, you'll see an interactive menu:
```
TFTP Client started
Server: localhost:6969

Commands:
1. Download file (GET)
2. Upload file (PUT)
3. Exit
Enter choice:
```

- **Download a file (GET)**: Choose option `1` and enter the remote filename (on the server) and local filename (where to save it).
- **Upload a file (PUT)**: Choose option `2` and enter the local filename (to upload) and remote filename (where to save it on the server).
- **Exit**: Choose option `3` to quit the client.

### Notes
- The server runs on port `6969` by default.
- Ensure the server is running before starting the client.
- Files uploaded/downloaded are stored in the working directory of the client/server.

## Project Structure
```
src/main/java/com/networkcourse/tftp/
├── common/       # Common interfaces and constants
├── client/       # Client application
├── server/       # Server application
└── util/         # Utility classes
```
