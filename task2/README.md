# TFTP-like Implementation over TCP (Task 2)

This project implements a simplified TFTP-like protocol over TCP as specified in Task 2 of the Computer Networks assignment.

## Features
- TCP-based TFTP-like implementation
- Basic read/write operations
- Leverages TCP's reliability (no need for ACKs or retransmissions)
- Console-based client and server applications
- Support for simultaneous file transfers with multiple clients

## How to Run
### 1. Compile the Project
Run the following command in the `task2` directory:
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

## Testing the Implementation

### Step 1: Create a Test Folder and File
1. Create a test folder:
   ```bash
   mkdir -p /home/davinci/Desktop/Java-Net/test_files
   ```

2. Create a sample file:
   ```bash
   echo "This is a test file for TFTP." > /home/davinci/Desktop/Java-Net/test_files/test.txt
   ```

### Step 2: Upload the Test File
1. Start the TCP TFTP Server:
   ```bash
   java -cp target/classes com.networkcourse.tftp.server.TFTPServer
   ```

2. Run the client and choose option `2` (Upload):
   ```bash
   java -cp target/classes com.networkcourse.tftp.client.TFTPClient localhost 6969
   ```
   - Local filename: /home/davinci/Desktop/Java-Net/test_files/test.txt
   - Remote filename: test.txt

### Step 3: Download the Test File
1. Run the client and choose option `1` (Download):
   ```bash
   java -cp target/classes com.networkcourse.tftp.client.TFTPClient localhost 6969
   ```
   - Remote filename: test.txt
   - Local filename: /home/davinci/Desktop/Java-Net/test_files/downloaded_test_tcp.txt

2. Verify the downloaded file:
   ```bash
   cat /home/davinci/Desktop/Java-Net/test_files/downloaded_test_tcp.txt
   ```

## Project Structure
```
src/main/java/com/networkcourse/tftp/
├── common/       # Common interfaces and constants
├── client/       # Client application
├── server/       # Server application
└── util/         # Utility classes
