package com.panda.flashlocaldownloadserver.utils.yt_dlp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class YtDlpManager {
    private static final String YT_DLP_BINARY = "yt-dlp.exe";
    private static final String FFMPEG_BINARY = "ffmpeg.exe";

    private static final String LOCAL_DIR = System.getProperty("user.home") + "/.flash_file_downloader/bin";
    private static final File YT_DLP_FILE = new File(LOCAL_DIR, YT_DLP_BINARY);
    private static final File FFMPEG_FILE = new File(LOCAL_DIR, FFMPEG_BINARY);

    private static final String YT_DLP_RESOURCE = "/com/panda/flashlocaldownloadserver/bin/" + YT_DLP_BINARY;
    private static final String FFMPEG_RESOURCE = "/com/panda/flashlocaldownloadserver/bin/" + FFMPEG_BINARY;

    private static File extractBinary(String resourcePath, File targetFile) throws IOException {
        if (targetFile.exists()) return targetFile;
        targetFile.getParentFile().mkdirs();

        try (InputStream in = YtDlpManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new FileNotFoundException(resourcePath + " not found in resources!");
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        targetFile.setExecutable(true);
        return targetFile;
    }

    private static void ensureBinaries() throws IOException {
        extractBinary(YT_DLP_RESOURCE, YT_DLP_FILE);
        extractBinary(FFMPEG_RESOURCE, FFMPEG_FILE);
    }

    public static String getYtDlpPath() throws IOException {
        ensureBinaries();
        return YT_DLP_FILE.getAbsolutePath();
    }

    public static String getFfmpegPath() throws IOException {
        ensureBinaries();
        return FFMPEG_FILE.getAbsolutePath();
    }

    public static void updateYtDlp() throws IOException {
        ensureBinaries();
        ProcessBuilder pb = new ProcessBuilder(YT_DLP_FILE.getAbsolutePath(), "-U");
        pb.inheritIO();
        pb.start();
    }

    public static String getDirectVideoUrl(String videoUrl) throws IOException {
        ensureBinaries();
        List<String> command = Arrays.asList(
                YT_DLP_FILE.getAbsolutePath(),
                "--ffmpeg-location", FFMPEG_FILE.getAbsolutePath(),
                "-f", "best",
                "-g",
                videoUrl
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String directUrl = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    directUrl = line;
                }
            }
        }
        return directUrl;
    }
}
