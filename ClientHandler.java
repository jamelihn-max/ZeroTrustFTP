package ztftpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    String sessionKey = null;
    private SecurityManager sm;
    private int keyFailureCount = 0;
    private static final int MaxKeyFailures = 3;
    private static final String SERVER_FILE_ROOT = "ServerFiles/";

    Socket client;

    ClientHandler(Socket skt, SecurityManager sm) {
        this.client = skt;
        this.sm = sm;
    }//constructor

    @Override
    public void run() {
        try {
            BufferedReader R = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            PrintWriter p = new PrintWriter(this.client.getOutputStream(), true);
            System.out.println("New client connected: " + client.getInetAddress());

            p.println(" Welcome to ZT-FTP. Please login using: AUTH <user> <pass> , or QUIT to exit.");

            String clientRequest = R.readLine();

            while (!this.client.isClosed() && (clientRequest) != null) {

                System.out.println("Received: " + clientRequest);

                String[] parts = clientRequest.split(" ");
                String command = parts[0].toUpperCase();

                if (command.equals("AUTH")) {
                    handleAuthentication(parts, p);
                } else if (command.equals("QUIT")) {
                    p.println(" Goodbye.");
                    break;
                } else if (this.sessionKey == null) {

                    p.println("Error: Authentication required. Please log in first.");
                } else {
                    handleSecuredCommand(parts, p);
                }
                clientRequest = R.readLine();
            } //  while
        }//try
        catch (IOException e) {
            System.out.println("Client disconnected or I/O error on " + client.getInetAddress()
                    + ": " + e.getMessage());
        }//catch
        finally {
            this.sm.removeSession(this.sessionKey);
            try {
                this.client.close();
            } catch (IOException ex) {
                System.out.println("Error in closing client connection: " + ex);
            }
            System.out.println("Connection closed for " + client.getInetAddress());
        }//finally
    }//run

    private void handleAuthentication(String[] parts, PrintWriter p) {

        if (parts.length != 3) {
            p.println(" Syntax error: AUTH <user> <pass>");
            return;
        }
        String newKey = this.sm.authenticate(parts[1], parts[2]);

        if (newKey != null) {
            this.sessionKey = newKey;
            this.keyFailureCount = 0;
            sm.logEvent("AUTH_SUCCESS", " User '" + parts[1] + "' logged in.");
            p.println(" Login successful. Session Key: " + newKey);
        } else {
            sm.logEvent("AUTH_FAIL", "Failed login attempt for user: '" + parts[1] + "'");

            p.println(" Login failed. Invalid credentials.");
        }

    }//handleAuthentication

    private void handleSecuredCommand(String[] parts, PrintWriter p) {
        if (parts.length < 2) {
            p.println("Syntax error. Command must include the Session Key.");
            return;
        }
        String receivedKey = this.sessionKey;
        String command = parts[0].toUpperCase();
        String filename = parts.length > 2 ? parts[2] : null;

        if (!this.sm.checkSessionKey(receivedKey)) {

            this.keyFailureCount++;
            System.out.println("Invalid Key attempt from " + client.getInetAddress()
                    + ". Count: " + this.keyFailureCount);

            if (this.keyFailureCount >= MaxKeyFailures) {
                sm.logEvent("SECURITY_ALERT", "Connection terminated for " + client.getInetAddress()
                        + " after 3 failed key attempts.");
                p.println(" Too many invalid attempts. Connection closing.");
                try {
                    this.client.close();
                } catch (IOException e) {
                    System.out.println("Error closing client connection: " + e.getMessage());
                }
            }
            return;
        }//if

        this.keyFailureCount = 0;

        if (!this.sm.authorizeAction(receivedKey, command)) {
            String role = this.sm.getUserRole(receivedKey);
            p.println("Access Denied. Your role (" + (role != null ? role : "Unknown") + ") is not authorized to use the command " + command + ".");
            return;
        }

        String currentUsername = this.sm.getUsername(receivedKey);
        switch (command) {
            case "LIST":
                String targetUser = parts.length > 2 ? parts[2] : null;
                handleList(p, currentUsername, targetUser);
                break;
            case "UPLOAD":
                handleUpload(parts, p, currentUsername);
                break;
            case "DOWNLOAD":
                handleDownload(parts, p, currentUsername);
                break;
            case "DELETE":
                handleDelete(parts, p, currentUsername, filename);
                break;
            default:
                p.println(" Command not implemented: " + command);
                break;
        }//switch

    }//handleSecuredCommand

    private void handleList(PrintWriter p, String currentUsername, String targetUsername) {
        String folderToAccess = currentUsername; 
        String role = this.sm.getUserRole(this.sessionKey);

        if (targetUsername != null) {
            if ("Super".equalsIgnoreCase(role)) {
                folderToAccess = targetUsername;
            } else {
                p.println("Access Denied: Only Super users can list files for other accounts.");
                return;
            }
        }

        File userDir = new File(SERVER_FILE_ROOT + folderToAccess);

        if (!userDir.exists()) {
            p.println("Error: Directory for '" + folderToAccess + "' does not exist.");
            return;
        }

        File[] files = userDir.listFiles();

        if (files == null || files.length == 0) {
            p.println(" Directory '" + folderToAccess + "' is empty.");
        } else {
            p.println(" File list for '" + folderToAccess + "' follows (" + files.length + " items):");

            for (File file : files) {
                if (file.isFile()) {
                    p.println("FILE: " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
            p.println(" End of file list.");
        }
    }

    private void handleUpload(String[] parts, PrintWriter p, String currentUsername) {
        if (parts.length < 3) {
            p.println(" Syntax error: UPLOAD <key> <filename>");
            return;
        }
        String filename = parts[2];

        File newFile = new File(SERVER_FILE_ROOT + currentUsername + File.separator + filename);

        p.println(" Ready to receive file " + filename);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile); java.io.InputStream is = client.getInputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            p.println(" Transfer complete. File saved.");
            System.out.println("File " + filename + " received successfully for " + currentUsername);

        } catch (IOException e) {
            p.println(" Error during file transfer: " + e.getMessage());
            System.out.println("Upload Error: " + e.getMessage());
            newFile.delete();
        }
    }//handleUpload

    private void handleDownload(String[] parts, PrintWriter p, String currentUsername) {
        if (parts.length < 3) {
            p.println(" Syntax error: DOWNLOAD <key> <filename>");
            return;
        }
        String filename = parts[2];
        File fileToDownload = new File(SERVER_FILE_ROOT + currentUsername + File.separator + filename);

        if (!fileToDownload.exists()) {
            p.println("Error: File '" + filename + "' not found or is a directory.");
            return;
        }

        long fileSize = fileToDownload.length();

        p.println(" File found. Size: " + fileSize);

        try (java.io.FileInputStream fis = new java.io.FileInputStream(fileToDownload); java.io.OutputStream os = client.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            p.println(" Transfer complete.");
            System.out.println("File " + filename + " sent successfully to " + currentUsername);

        } catch (IOException e) {
            p.println(" Error during file transfer: " + e.getMessage());
            System.err.println("Download Error: " + e.getMessage());
        }
    }//handleDownload

    private void handleDelete(String[] parts, PrintWriter p, String currentUsername, String filename) {
        if (parts.length < 3) {
            p.println(" Syntax error: DELETE <key> <filename> [targetUser]");
            return;
        }
        //ما رح يقدر يوصل ال normal client  لهون لانه ما عنده صلاحية
        // but for defense in depth we recheck the role
        String targetFolder = currentUsername;
        String fileToDelete = filename;
        String role = this.sm.getUserRole(this.sessionKey);
        if ("Super".equalsIgnoreCase(role) && parts.length == 4) {
            targetFolder = parts[3];
        }

        File targetFile = new File(SERVER_FILE_ROOT + targetFolder + File.separator + fileToDelete);

        if (!targetFile.exists()) {
            p.println(" Error: File '" + fileToDelete + "' not found in the folder:" + targetFolder);
            return;
        }
        if (targetFile.delete()) {
            String msg = "User '" + currentUsername + "' deleted '" + fileToDelete + "' from folder '" + targetFolder + "'";
            sm.logEvent("DELETE_SUCCESS", msg);
            p.println(" File '" + fileToDelete + "' deleted successfully from " + targetFolder + ".");
        } else {
            sm.logEvent("DELETE_FAIL", "User '" + currentUsername + "' failed to delete '" + fileToDelete + "'");
            p.println("Failed to delete file '" + fileToDelete + "'. Check permissions.");
        }
    }//handleDelete

}//class
