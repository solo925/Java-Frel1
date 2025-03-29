# TFTP Implementation in Java (Task 1)

This project implements the Trivial File Transfer Protocol (TFTP) as specified in RFC 1350. It focuses on Task 1 of the Computer Networks assignment.

## Features
- Complete UDP-based TFTP implementation following RFC 1350
- Support for octet mode only (raw byte transfers)
- Error handling for file not found
- Console-based client and server applications
- Support for simultaneous file transfers with multiple clients

## Project Structure
```
src/main/java/com/networkcourse/tftp/
├── common/       # Common interfaces and constants
├── client/       # Client application
├── server/       # Server application
└── util/         # Utility classes
```

## How to Run
### Server
```
java -cp target/classes com.networkcourse.tftp.server.TFTPServer [port]
```

### Client
```
java -cp target/classes com.networkcourse.tftp.client.TFTPClient [server_address] [server_port]
```

Default port is 6969 (above 1024 as required in the assignment).
