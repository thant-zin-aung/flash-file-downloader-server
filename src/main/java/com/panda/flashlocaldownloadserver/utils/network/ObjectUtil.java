package com.panda.flashlocaldownloadserver.utils.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.panda.flashlocaldownloadserver.utils.yt_dlp.YtDlpFormatFetcherJson;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectUtil {
    private static final Gson gson = new Gson();
    public static Map<String, String> queryToMap(String query) {
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""
                ));
    }

    public static String toJson(List<YtDlpFormatFetcherJson.Format> formats) {
        // Use manual JSON formatting or use a library like Gson
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < formats.size(); i++) {
            YtDlpFormatFetcherJson.Format f = formats.get(i);
            sb.append(String.format(
                    "{\"id\":\"%s\",\"resolution\":\"%s\",\"size\":\"%s\",\"type\":\"%s\"}",
                    f.getFormatId(), f.getResolution(), f.getFilesize(), f.getType()
            ));
            if (i < formats.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static Map<String, Object> jsonToMap(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
    }
}
