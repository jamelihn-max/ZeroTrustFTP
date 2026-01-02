package ztftpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {

    // Map to store valid users: username -> UserData
    private final Map<String, UserData> validUsers = new HashMap<>();
    // Map to store active sessions: sessionKey -> username
    //we use ConcurrentHashMap for thread safety
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

        public SecurityManager(){
                loadUsers();
        }//default constructor

    class UserData {
        String username;
        String password;
        String role; 

        public UserData(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }//UserData

    public String getUsername(String key) {
        if (key != null) {
            return activeSessions.get(key);
        }
        return null;
    }//getUsername

    public String getUserRole(String key) {
        String username = activeSessions.get(key);
        if (username != null) {
            UserData user = validUsers.get(username);
            return user != null ? user.role : null;
        }
        return null;
    }//getUserRole

    private void loadUsers() {
        System.out.println(" Loading users from users.txt...");
        //
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" "); 
                if (parts.length == 3) {
                    UserData user = new UserData(parts[0], parts[1], parts[2]);
                    validUsers.put(user.username, user);
                }
            }//while
            System.out.println("SecurityManager: " + validUsers.size() + " users loaded.");
        } catch (IOException e) {
            System.out.println("SecurityManager ERROR: Could not load users.txt. " + e.getMessage());
        }
    }//loadUsers

    public String authenticate(String username, String password){
        UserData user = validUsers.get(username);
        if(user != null && user.password.equals(password)){
            String newKey = UUID.randomUUID().toString();
            //UUID to generate unique session keys
            //Universal Unique Identifier 
            //Token based authentication
            activeSessions.put(newKey, username);
            System.out.println(" User " + username + " authenticated successfully");
            return newKey;
        }else{
            System.out.println(" Failed login attempt for " + username);
            return null;
        }
    } //authenticate

    public boolean checkSessionKey(String sessionKey){
        
        return sessionKey != null && activeSessions.containsKey(sessionKey);

    } //checkSessionKey

    public void removeSession(String key) {
        if (key != null) {
            activeSessions.remove(key);
            System.out.println(" Session key removed ");
        }
    }//removeSession

    public boolean authorizeAction(String sessionKey, String command){
        String username = activeSessions.get(sessionKey);
        UserData user = validUsers.get(username);
        if(user == null){
            return false;
        }
        if ("Super".equalsIgnoreCase(user.role)) {
            return true;
        }
       if ("Normal".equalsIgnoreCase(user.role)) {
            String upperCommand = command.toUpperCase();
            if (upperCommand.equals("UPLOAD") || 
                upperCommand.equals("DOWNLOAD") || 
                upperCommand.equals("LIST")) {
                return true; 
            }
       }
         return false;
    } //authorizeAction
// logging 
    public synchronized void logEvent(String eventType, String message) {
    String logEntry = String.format("[%s] [%s] %s", 
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
        eventType.toUpperCase(), 
        message);

    System.out.println(logEntry);
    
    try (BufferedWriter bw = new BufferedWriter(new FileWriter("logging.txt", true))) {
        bw.write(logEntry);
        bw.newLine();
    } catch (IOException e) {
        System.err.println("Logging Error: Could not write to logging.txt " + e.getMessage());
    }
}
}//class
