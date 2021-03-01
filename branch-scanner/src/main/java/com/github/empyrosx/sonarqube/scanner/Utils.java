package com.github.empyrosx.sonarqube.scanner;

import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
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

    public static WsResponse call(Object wsClient, WsRequest request) {
        try {
            return (WsResponse) wsClient.getClass().getMethod("call", WsRequest.class).invoke(wsClient, request);
        } catch (ReflectiveOperationException ex) {
//            ex.printStackTrace();
//            handleIfInvocationException(ex);
            throw (RuntimeException) ex.getCause();
//            throw new IllegalStateException("Could not execute ScannerWsClient", ex);
        }
    }

    private static void handleIfInvocationException(ReflectiveOperationException ex) {
        if (!(ex instanceof InvocationTargetException)) {
            return;
        }
        Throwable cause = ex.getCause();
        if (cause instanceof Error) {
            throw (Error) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
    }

}
