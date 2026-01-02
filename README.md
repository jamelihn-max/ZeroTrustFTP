Jamelih Nofal & Nour Aljuidy
1. Implemented Security Features:
    Server-Side:
        * Authentication: Clients must authenticate using a username/password (stored in users.txt) before accessing services.
        * Session Management: A unique UUID Session Key is issued upon login and must accompany every subsequent command.
        * Zero-Trust Validation: Every request is verified for a valid session key and appropriate user role before execution.
        * Role-Based Access Control: 
            Normal Users: Limited to UPLOAD, DOWNLOAD, and LIST on their dedicated files.
            Super Users: Full access to all commands and files across the server, can delete and list the files of any user.
		* Remove SessionKey when the client close his session, to not allow others access or use it as active session.
        * Three-Strike Rule: Connections are terminated after 3 consecutive invalid session key attempts.
        * Security Logging: Critical events (failed logins, deletions, and forced connection terminations) are logged in logging.txt with timestamps.
        
    Client-Side:
   * Modular Architecture: The client is structured into three distinct layers (ZTFTPClient, ClientService, and FileUtil) for better maintainability and professional code standards.

   * Binary Data Support: The system supports the transfer of all file types (images, documents, PDFs) using optimized Byte Streams.

   * Utility Tools (FileUtil): The client automatically verifies local file existence, calculates formatted sizes, and ensures the download directory exists before starting transfers.

2. Execution Instructions
    Prerequisites: 
        a. Ensure Java JDK is installed on your system.
		b. change the location of each folder (ZTFTPServer, ZTFTPClient) to NetbeansProjects.
        b. Create a folder named ServerFiles in the project root directory in case you didnot find it(you will find it in ZTFTPServer folder ).
        c. Inside ServerFiles, create subfolders for each user as defined in users.txt ( you will find these ServerFiles/user1/, ServerFiles/admin/ but if you want to add more users).
        d. you should have the file users.txt in the same directory of project files before running the server.
    Steps:
       a. Run the Server: Compile and run ZTFTPServer.java first. It will start listening on port 6000.

       b. Run the Client: Compile and run ZTFTPClient.java. Follow the on-screen prompts to log in and enter commands.

3. Project File StructureFile
   a. ZTFTPServer.java : The main server entry point that handles incoming socket connections.
   b. ClientHandler.java : Manages individual client requests using multi-threading.
   c. SecurityManager.java : The security engine responsible for session and role validation.
   d. ZTFTPClient.java : The main client entry point providing the Console User Interface.
   e. ClientService.java : Logic layer for executing commands like Upload, Download, and List.
   f. FileUtil.java : Helper class for managing local files and formatting file data.         

4. Known Limitations:
    a. User directories must be manually created on the server before use.


