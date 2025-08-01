package com.panda.flashlocaldownloadserver.utils.yt_dlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class YtDlpFormatFetcherJson {

    public static class Format {
        String formatId;
        String extension;
        String resolution;
        long filesize;
        String type;
        String note;

        public String getFormatId() {
            return formatId;
        }

        public String getExtension() {
            return extension;
        }

        public String getResolution() {
            return resolution;
        }

        public String getFilesize() {
            return filesize > 0 ? String.format("%.2f MB", filesize / 1024.0 / 1024.0) : "Unknown";
        }

        public String getType() {
            return type;
        }

        public String toString() {
            String sizeStr = filesize > 0 ? String.format("%.2f MB", filesize / 1024.0 / 1024.0) : "Unknown";
            return String.format("ID: %s | %s | %s | %s | %s", formatId, extension, resolution, sizeStr, type);
        }
    }

    private static List<Format> fetchFormats(String videoUrl) throws IOException {
        videoUrl = videoUrl.split("&")[0];

        ProcessBuilder pb = new ProcessBuilder(
                YtDlpManager.getYtDlpPath(),
                "-J",
                "--no-warnings",
                "--no-playlist",
                "--skip-unavailable-fragments",
                "--force-ipv4",
                "-f", "bv*+ba/b",
                videoUrl
        );
//        pb.redirectErrorStream(true);

        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            output = sb.toString().trim();
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0 || !output.startsWith("{")) {
                return Collections.emptyList();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(output);

        List<Format> result = new LinkedList<>();
        for (JsonNode f : root.get("formats")) {
            Format format = new Format();
            format.formatId = f.get("format_id").asText();
            format.extension = f.get("ext").asText();
            format.resolution = f.hasNonNull("height") ? f.get("height").asText() + "p" : "audio only";
            format.filesize = f.has("filesize") && !f.get("filesize").isNull() ? f.get("filesize").asLong() : -1;
            format.type = f.has("vcodec") && f.get("vcodec").asText().equals("none") ? "audio" : "video";
            result.add(format);
        }

        return result;
    }


    private static Map<String, Format> groupBestByResolution(List<Format> formats) {
        Map<String, Format> best = new LinkedHashMap<>();

        for (Format f : formats) {
            String key = f.type.equals("audio") ? "audio:" + f.extension : f.resolution;
            if (!best.containsKey(key) || isBetterFormat(f, best.get(key))) {
                best.put(key, f);
            }
        }

        return best;
    }

    private static boolean isBetterFormat(Format a, Format b) {
        return a.filesize > b.filesize; // Pick larger file (usually better quality)
    }


    public static Map<String, Format> getFormats(String videoUrl) throws IOException {
        List<Format> formats = fetchFormats(videoUrl);
        return groupBestByResolution(formats);
    }
}
