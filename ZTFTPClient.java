package ztftpclient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ZTFTPClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 6000;
    private static String sessionKey = null;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to ZT-FTP Server.");
            System.out.println("Server: " + in.readLine());

            boolean authenticated = false;
            while (!authenticated) {
                System.out.println("\nOptions: AUTH (to login), QUIT (to exit)");
                System.out.print("Enter choice: ");
                String choice = scanner.nextLine().toUpperCase();

                if (choice.equals("QUIT")) {
                    out.println("QUIT");
                    System.out.println("Exiting... Goodbye!");
                    return;
                }

                if (choice.equals("AUTH")) {
                    System.out.print("Enter username: ");
                    String user = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String pass = scanner.nextLine();

                    out.println("AUTH " + user + " " + pass);
                    String response = in.readLine();
                    System.out.println("Server: " + response);

                    if (response != null && response.contains("Login successful")) {
                        sessionKey = response.split(": ")[1].trim();
                        authenticated = true;
                    } else {
                        System.out.println("Authentication failed. Try again.");
                    }
                } else {
                    System.out.println("Invalid option. Please type AUTH or QUIT.");
                }
            }//while
            ClientService service = new ClientService(socket, out, in);
            while (true) {
                System.out.println("\nCommands: LIST, DOWNLOAD, UPLOAD, DELETE, QUIT");
                System.out.print("Enter command: ");
                String userInput = scanner.nextLine();
                String[] parts = userInput.split(" ");
                String command = parts[0].toUpperCase();

                if (command.equals("QUIT")) {
                    out.println("QUIT");
                    System.out.println("Server says: " + in.readLine());
                    break;
                }

                switch (command) {
                    case "LIST":
                        String listTarget = (parts.length >= 2) ? parts[1] : null;
                        service.listFiles(sessionKey, listTarget);
                        break;
                    case "DOWNLOAD":
                        if (parts.length < 2) {
                            System.out.println("Usage: DOWNLOAD <filename>");
                        } else {
                            service.downloadFile(sessionKey, parts[1]);
                        }
                        break;
                    case "UPLOAD":
                        if (parts.length < 2) {
                            System.out.println("Usage: UPLOAD <filename>");
                        } else {
                            service.uploadFile(sessionKey, parts[1]);
                        }
                        break;
                    case "DELETE":
                        if (parts.length < 2) {
                            System.out.println("Usage: DELETE <filename> [targetUser]");
                        } else {
                            String target = (parts.length == 3) ? parts[2] : null;
                            service.deleteFile(sessionKey, parts[1], target);
                        }
                        break;
                    default:
                        System.out.println("Invalid command.");
                }
            }
        } catch (IOException e) {
            System.err.println("Client Error: " + e.getMessage());
        }
    }
}
