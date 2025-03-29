# TFTP Server and Client Interoperability Guide

This guide explains how to test the interoperability of the TFTP server and client with third-party implementations.

---

## Prerequisites
1. **Java Development Kit (JDK)**: Ensure JDK is installed.
2. **Third-Party TFTP Server/Client**: Install `tftpd-hpa` (server) and `tftp` (client) for testing.

---

## Step 1: Start the TFTP Server
1. Navigate to the `task2` directory:
   ```bash
   cd /home/davinci/Desktop/Java-Net/task2
   ```

2. Compile the server:
   ```bash
   mvn compile
   ```

3. Start the server:
   ```bash
   java -cp target/classes com.networkcourse.tftp.server.TFTPServer
   ```

   The server will start on port `6969` with the base directory set to `/home/davinci/Desktop/Java-Net/task2`.

---

## Step 2: Test with Third-Party TFTP Client
1. Install the `tftp` client:
   ```bash
   sudo apt install tftp
   ```

2. Navigate to the directory containing the file you want to upload:
   ```bash
   cd /home/davinci/Desktop/Java-Net/task3
   ```

3. Start the `tftp` client and connect to the server:
   ```bash
   tftp localhost 6969
   ```

4. Upload a file:
   ```bash
   tftp> put file.txt
   ```

5. Download a file:
   ```bash
   tftp> get remote_file.txt
   ```

---

## Step 3: Test with Third-Party TFTP Server
1. Install `tftpd-hpa`:
   ```bash
   sudo apt update
   sudo apt install tftpd-hpa
   ```

2. Configure the server:
   - Edit `/etc/default/tftpd-hpa`:
     ```bash
     TFTP_USERNAME="tftp"
     TFTP_DIRECTORY="/srv/tftp"
     TFTP_ADDRESS=":69"
     TFTP_OPTIONS="--secure"
     ```

   - Create the TFTP directory and set permissions:
     ```bash
     sudo mkdir -p /srv/tftp
     sudo chown -R tftp:tftp /srv/tftp
     sudo chmod -R 777 /srv/tftp
     ```

   - Restart the service:
     ```bash
     sudo systemctl restart tftpd-hpa
   ```

3. Place a test file in the TFTP directory:
   ```bash
   echo "Hello, TFTP!" > /srv/tftp/test.txt
   ```

4. Use your TFTP client to download the file:
   ```bash
   java -cp target/classes com.networkcourse.tftp.client.TFTPClient localhost 69
   ```

   Choose the download option and specify `test.txt` as the remote file.

---

## Step 4: Verify Interoperability
1. **Client-Side**:
   - Ensure your client can download files from the third-party server.
   - Check for any errors or mismatches in file content.

2. **Server-Side**:
   - Ensure the third-party client can upload and download files from your server.
   - Verify file integrity and correct handling of errors (e.g., file not found).

---

## Troubleshooting
- **File Not Found**:
  - Ensure the file exists in the specified directory.
  - Check file permissions:
    ```bash
    chmod 644 /path/to/file.txt
    ```

- **Connection Issues**:
  - Verify the server is running and listening on the correct port.
  - Check firewall settings to ensure the port is not blocked.

---

## Conclusion
This guide demonstrates how to test the interoperability of your TFTP server and client with third-party implementations. Update this README with your test results and any issues encountered.
