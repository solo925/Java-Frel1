# TFTP-like Implementation over TCP (Task 2)

This project implements a simplified TFTP-like protocol over TCP as specified in Task 2 of the Computer Networks assignment.

## Features
- TCP-based TFTP-like implementation
- Basic read/write operations
- Leverages TCP's reliability (no need for ACKs or retransmissions)
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
