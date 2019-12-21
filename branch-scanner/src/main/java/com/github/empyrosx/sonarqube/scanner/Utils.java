package com.github.empyrosx.sonarqube.scanner;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils {

    private Utils() {
    }

    public static String encodeForUrl(@Nullable String url) {
        try {
            return URLEncoder.encode(url == null ? "" : url, StandardCharsets.UTF_8.name());

        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Encoding is not supported", e);
        }
    }
}
