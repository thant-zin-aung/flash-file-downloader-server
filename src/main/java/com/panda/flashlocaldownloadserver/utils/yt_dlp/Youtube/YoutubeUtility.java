package com.panda.flashlocaldownloadserver.utils.yt_dlp.Youtube;


import com.panda.flashlocaldownloadserver.utils.ObservableValue;
import com.panda.flashlocaldownloadserver.utils.yt_dlp.YtDlpManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtility {

    public static final ObservableValue<String> fileNameObserver = new ObservableValue<>();
    public static final ObservableValue<String> fileSizeObserver = new ObservableValue<>();
    public static final ObservableValue<Double> progressPercentObserver = new ObservableValue<>();
    public static final ObservableValue<String> connectionSpeedObserver = new ObservableValue<>();
    public static final ObservableValue<String> etaObserver = new ObservableValue<>();
    public static final ObservableValue<Boolean> downloadFinishStatusObserver = new ObservableValue<>();

    public record DownloadResult(String filename, int exitCode) {}
    private static double audioSizeMB = 0.0;
    private static double videoSizeMB = 0.0;
    private static boolean audioDownloaded = false;
    private static boolean videoDownloaded = false;
    public static DownloadResult downloadAndMerge(String videoFormatId, String youtubeUrl, String outputDir) throws Exception {
        resetFileSizeTracking();
        downloadFinishStatusObserver.setValue(false);
        String outputTemplate = outputDir + "/%(title)s.%(ext)s";

        ProcessBuilder pb = new ProcessBuilder(
                YtDlpManager.getYtDlpPath(),
                "--ffmpeg-location", YtDlpManager.getFfmpegPath(),
                "-f", videoFormatId + "+bestaudio",
                "--merge-output-format", "mp4",
                "--output", outputTemplate,
                youtubeUrl
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        String finalMergedFilename = null;
        Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d{1,3}(?:\\.\\d+)?)% of (.+?) at (.+?) ETA (.+)");
        Pattern mergerPattern = Pattern.compile("\\[Merger\\] Merging formats into \"(.+?)\"");
        Pattern destinationPattern = Pattern.compile("\\[download\\] Destination: (.+)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);

                // Parse download destination
                Matcher destinationMatcher = destinationPattern.matcher(line);
                if (destinationMatcher.find()) {
                    String rawPath = destinationMatcher.group(1).trim();
                    String baseName = new File(rawPath).getName();
                    baseName = baseName.replaceAll("\\.f\\d+\\.(mp4|webm|m4a)$", ".mp4");
                    String cleanName = sanitizeFileName(baseName);
                    fileNameObserver.setValue(cleanName);
                }

                // Parse merging result
                Matcher mergerMatcher = mergerPattern.matcher(line);
                if (mergerMatcher.find()) {
                    String mergedPath = mergerMatcher.group(1).trim();
                    finalMergedFilename = sanitizeFileName(new File(mergedPath).getName());
                    fileNameObserver.setValue(finalMergedFilename);
                }

                handleProgressLine(line, progressPattern);
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Download finished with exit code: " + exitCode);
        downloadFinishStatusObserver.setValue(true);
        return new DownloadResult(finalMergedFilename, exitCode);
    }

    private static void handleProgressLine(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            try {
                double percent = Double.parseDouble(matcher.group(1));
                String fileSizeStr = matcher.group(2);
                double sizeMB = parseSizeInMB(fileSizeStr);

                // Decide whether current is video or audio
                if (!videoDownloaded && !audioDownloaded) {
                    videoSizeMB = sizeMB;
                    videoDownloaded = true;

                    // Show video size initially
                    fileSizeObserver.setValue(String.format("%.2f MiB", videoSizeMB));

                } else if (videoDownloaded && !audioDownloaded && sizeMB != videoSizeMB) {
                    audioSizeMB = sizeMB;
                    audioDownloaded = true;

                    // Show combined size once audio appears
                    double totalMB = videoSizeMB + audioSizeMB;
                    fileSizeObserver.setValue(String.format("%.2f MiB", totalMB));
                }

                progressPercentObserver.setValue(percent);
                connectionSpeedObserver.setValue(matcher.group(3));
                etaObserver.setValue(matcher.group(4));
            } catch (NumberFormatException ignored) {}
        }
    }


    private static double parseSizeInMB(String sizeStr) {
        try {
            sizeStr = sizeStr.trim().replaceAll(",", "");
            if (sizeStr.endsWith("KiB")) {
                return Double.parseDouble(sizeStr.replace("KiB", "").trim()) / 1024.0;
            } else if (sizeStr.endsWith("MiB")) {
                return Double.parseDouble(sizeStr.replace("MiB", "").trim());
            } else if (sizeStr.endsWith("GiB")) {
                return Double.parseDouble(sizeStr.replace("GiB", "").trim()) * 1024.0;
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return 0.0;
    }


    private static void resetFileSizeTracking() {
        audioSizeMB = 0.0;
        videoSizeMB = 0.0;
        audioDownloaded = false;
        videoDownloaded = false;

    }


    private static String sanitizeFileName(String name) {
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        name = name.replaceAll("\\s{2,}", " ").trim();
        name = name.replaceAll("^[\\s\\-_.]+", "");
        return name;
    }
}
