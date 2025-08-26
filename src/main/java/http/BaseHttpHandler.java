package http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.awt.SystemColor.text;

public class BaseHttpHandler {

    protected void addCors(HttpExchange h) {
        var headers = h.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Access-Control-Max-Age", "86400");
        headers.add("Vary", "Origin");
        headers.add("Content-Type", "application/json; charset=utf-8");
    }

    // quick response to preflight
    protected void handlePreflight(HttpExchange h) throws IOException {
        addCors(h);
        h.sendResponseHeaders(204, -1); // No Content
        h.close();
    }

    protected void sendText(HttpExchange h, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(200, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected void sendResponse(HttpExchange h, int code, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(code, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    public void sendNotFound(HttpExchange ex, int status, String text) throws IOException {
        sendResponse(ex, status, text);
    }

    public void sendHasOverlaps(HttpExchange ex, String text) throws IOException {
        sendResponse(ex, 406, text);
    }
}