package ztftpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ZTFTPServer {

    public static void main(String[] args) {
        ServerSocket server = null;
        SecurityManager sm = new SecurityManager();
        try{
            server = new ServerSocket(6000);
            System.out.println("ZT-FTP Server is running on port 6000...");
            
            while(true){
                Socket client = server.accept();
                ClientHandler ThClient = new ClientHandler(client, sm);
                ThClient.start();
            }//while
        }//try
        catch(IOException e){
            System.out.println(" Error from catch in main");
        }//catch
        finally{
                try{
                    System.out.println(" Server shutting down.");
                    server.close();
                }
            catch(IOException ex){
                System.out.println("Error closing server: "+ ex);}
        }
    }//main
    
}//class
