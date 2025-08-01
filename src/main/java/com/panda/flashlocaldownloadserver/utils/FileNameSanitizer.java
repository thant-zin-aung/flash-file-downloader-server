package com.panda.flashlocaldownloadserver.utils;

public class FileNameSanitizer {
    public static String sanitize(String filename) {
        if (filename == null) return null;
        String clean = filename.replaceAll("[<>:\"/\\\\|?*]", "");
        return clean.length() > 120 ? clean.substring(0, 120) : clean;
    }
}
