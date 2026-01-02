package ztftpclient;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientService {
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public ClientService(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }
    public void listFiles(String sessionKey, String targetUser) {
    try {
        String command = "LIST " + sessionKey;
        if (targetUser != null && !targetUser.isEmpty()) {
            command += " " + targetUser;
        }
        
        out.println(command);
        
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);

            if (line.contains("End of file list") || 
                line.contains("Directory is empty") || 
                line.contains("Error") || line.contains("Access Denied")) {
                break;
            }
        }
    } catch (IOException e) {
        System.out.println("Error while fetching file list: " + e.getMessage());
    }
}//listF

    public void uploadFile(String sessionKey, String filename) {
    // نستخدم FileUtil للتحقق من وجود الملف محلياً
    if (!FileUtil.fileExists(filename)) {
        System.out.println("Local Error: File '" + filename + "' not found on your device.");
        return;
    }

    File file = new File(filename);

    try {
        out.println("UPLOAD " + sessionKey + " " + filename);
        
        String response = in.readLine();
        System.out.println("Server: " + response);

        if (response != null && response.contains("Ready")) {
            long fileSize = FileUtil.getFileSize(filename);
            System.out.println("Client: Starting upload (" + FileUtil.formatSize(fileSize) + ")...");

            try (FileInputStream fis = new FileInputStream(file)) {
                OutputStream os = socket.getOutputStream(); 
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                os.flush(); 
                System.out.println("Client: File sent successfully.");
            }
        }
    } catch (IOException e) {
        System.out.println("Error during upload: " + e.getMessage());
    }
}//uploadFile

   public void downloadFile(String sessionKey, String filename) {
    try {
        out.println("DOWNLOAD " + sessionKey + " " + filename);
        String response = in.readLine();
        System.out.println("Server: " + response);

        if (response != null && response.startsWith(" File found")) {
            long fileSize = Long.parseLong(response.split(": ")[1].trim());
            
            FileUtil.checkAndCreateDirectory("DownloadedFiles");
            
            File targetFile = new File("DownloadedFiles", filename);
            
            // ملاحظة: لا نغلق socket.getInputStream() لأنه سيغلق السوكيت بالكامل
            // نكتفي بفتحFileOutputStream وحلقه القراءة
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                InputStream is = socket.getInputStream(); 
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;

                while (totalRead < fileSize) {
                    // حساب الكمية المتبقية للقراءة لضمان عدم سحب بيانات أوامر أخرى
                    int toRead = (int) Math.min(buffer.length, fileSize - totalRead);
                    bytesRead = is.read(buffer, 0, toRead);
                    
                    if (bytesRead == -1) break; // انقطع الاتصال فجأة
                    
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                System.out.println("Client: Download complete. Saved as: " + targetFile.getPath());
            }
        }
    } catch (IOException e) {
        System.out.println("Error during download: " + e.getMessage());
    }
}//downloadFile
    public void deleteFile(String sessionKey, String filename, String targetUser) {
        try {
            String command = "DELETE " + sessionKey + " " + filename;
            if (targetUser != null && !targetUser.isEmpty()) {
                command += " " + targetUser;
            }
            out.println(command);
            System.out.println("Server: " + in.readLine());
        } catch (IOException e) {
            System.out.println("Error during delete: " + e.getMessage());
        }
    }//deleteFile
}//ClientService.java
