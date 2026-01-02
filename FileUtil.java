package ztftpclient;

import java.io.File;

public class FileUtil {

    public static void checkAndCreateDirectory(String dirName) {
        File directory = new File(dirName);
        if (!directory.exists()) {
            if (directory.mkdir()) {
                System.out.println("[FileUtil] Created directory: " + dirName);
            }
        }
    }//checkAndCreateDirectory

    public static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists() && file.isFile();
    }//fileExists

    public static long getFileSize(String fileName) {
        File file = new File(fileName);
        return file.exists() ? file.length() : 0;
    }//getFileSize

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }//formatSize
}//FileUtil.java
