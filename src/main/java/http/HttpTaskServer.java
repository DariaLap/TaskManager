package http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import manager.Managers;
import manager.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import model.Epic;
import model.SubTask;
import model.Task;

public class HttpTaskServer {
    private static final int PORT = 8080;
    static TaskManager manager = Managers.getDefault();
    static HttpServer server;
    private static final Gson gson = new GsonBuilder()
            // LocalDateTime
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, t, c) -> src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, t, c) -> json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString()))
            // to ensure subtasks != null, otherwise it doesn't work
            .registerTypeAdapter(Epic.class, (InstanceCreator<Epic>) type -> new Epic("", ""))
            // Duration
            .registerTypeAdapter(Duration.class, (JsonSerializer<Duration>)
                    (src, t, c) -> src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Duration.class, (JsonDeserializer<Duration>)
                    (json, t, c) -> {
                        if (json == null || json.isJsonNull()) return null;
                        String s = json.getAsString();
                        if (s.startsWith("PT")) return Duration.parse(s); // ISO-8601
                        return Duration.ofMinutes(Long.parseLong(s));     // number of minutes
                    })
            .create();


    public static void main(String[] args) throws IOException {
        server = HttpServer.create();

        server.bind(new InetSocketAddress(PORT), 0); // bind server to port
        server.createContext("/tasks", new TasksHandler()).getFilters().add(new CorsFilter());
        server.createContext("/subtasks", new SubTasksHandler()).getFilters().add(new CorsFilter());
        server.createContext("/epics", new EpicsHandler()).getFilters().add(new CorsFilter());
        server.createContext("/history", new HistoryHandler()).getFilters().add(new CorsFilter());
        server.createContext("/prioritized", new PrioritizedHandler()).getFilters().add(new CorsFilter());

        server.start(); // start server
    }

    public static Gson getGson() {
        return gson;
    }

    public HttpTaskServer(TaskManager manager) throws IOException {
        HttpTaskServer.manager = manager;
        server = HttpServer.create();
        server.bind(new InetSocketAddress(PORT), 0); // bind server to port
        server.createContext("/tasks", new TasksHandler()); // bind path and handler
        server.createContext("/subtasks", new SubTasksHandler());
        server.createContext("/epics", new EpicsHandler());
        server.createContext("/history", new HistoryHandler());
        server.createContext("/prioritized", new PrioritizedHandler());
    }

    public void start() {
        server.start();
    }

    public void stop()  {
        server.stop(0);
    }

    static class TasksHandler extends BaseHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            System.out.println("Started handling /tasks request from client.");

            String method = ex.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePreflight(ex);
                return;
            }
            switch (method) {
                case "GET":
                    handleGet(ex);
                    break;
                case "POST":
                    handlePost(ex);
                    break;
                case "DELETE":
                    handleDelete(ex);
                    break;
                default:
                    sendNotFound(ex,405,"Nothing was found for your request");
                    return;

            }
        }

        public Integer identifyId(HttpExchange ex) throws IOException {
            String p = ex.getRequestURI().getPath();
            String[] parts = p.split("/");
            if (parts.length >= 3) {
                try {
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "Invalid request");
                }
            }
            return null;
        }

        public void handleGet(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                String json = gson.toJson(manager.getAllTasks());
                sendResponse(ex, 200, json);
            } else {
                Task t = manager.getTask(id);
                if (t == null) {
                    sendNotFound(ex, 404, "No such task");
                } else {
                    String json = gson.toJson(t);
                    sendResponse(ex, 200, json);
                }
            }
        }

        public void handlePost(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    Task task = gson.fromJson(body, Task.class);
                    task.setId(-1);
                    manager.addTask(task);
                    System.out.println(task);
                    sendResponse(ex, 201, "Task " + task.getId() + " successfully added");
                } catch (IllegalArgumentException e) {
                    sendHasOverlaps(ex, e.getMessage());
                } catch (Exception e) {
                    sendResponse(ex, 400, e.getMessage());
                    return;
                }

            } else {
                InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Task task = gson.fromJson(body, Task.class);
                if (manager.getTask(id) != null) {
                    manager.updateTask(id, task);
                    sendText(ex, "Task " + task.getId() + " successfully updated");
                } else {
                    sendNotFound(ex, 404, "Task not found");
                }

            }
        }

        public void handleDelete(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                addCors(ex);
                ex.sendResponseHeaders(204, -1); // No Content
                ex.close();
                return;
            }
            Integer id = identifyId(ex);
            if (id == null) {
                sendNotFound(ex, 404, "Task id not provided");
            } else {
                if (manager.getTask(id) != null) {
                    manager.deleteByID(id);
                    sendText(ex, "Task " + id + " successfully deleted");
                } else {
                    sendNotFound(ex, 404, "No such task");
                }
            }
        }
    }

    static class SubTasksHandler extends BaseHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            System.out.println("Started handling /subtasks request from client.");

            String method = ex.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePreflight(ex);
                return;
            }
            switch (method) {
                case "GET" -> handleGet(ex);
                case "POST" -> handlePost(ex);
                case "DELETE" -> handleDelete(ex);
                default -> {
                    sendNotFound(ex, 405, "Nothing was found for your request");
                    return;
                }
            }
        }

        public Integer identifyId(HttpExchange ex) throws IOException {
            String p = ex.getRequestURI().getPath();
            String[] parts = p.split("/");
            if (parts.length >= 3) {
                try {
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "Invalid request");
                }
            }
            return null;
        }

        public void handleGet(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                String json = gson.toJson(manager.getSubTasks());
                sendResponse(ex, 200, json);
            } else {
                SubTask t = (SubTask) manager.getTask(id);
                if (t == null) {
                    sendNotFound(ex, 404, "No such subtask");
                } else {
                    String json = gson.toJson(t);
                    sendResponse(ex, 200, json);
                }
            }
        }

        public void handlePost(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    SubTask task = gson.fromJson(body, SubTask.class);
                    task.setId(-1);
                    if (manager.getTask(task.getEpicId()) != null) {
                        manager.addTask(task);
                        sendResponse(ex, 201,"Subtask " + task.getId() + " successfully added");
                    } else {
                        sendNotFound(ex, 404,"Epic with id " + task.getEpicId() + " not found");
                    }
                } catch (IllegalArgumentException e) {
                    sendHasOverlaps(ex, e.getMessage());
                } catch (Exception e) {
                    sendResponse(ex, 400, e.getMessage());
                    return;
                }
            } else {
                InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                SubTask task = gson.fromJson(body, SubTask.class);
                System.out.println(task.toString());
                if (manager.getTask(id) != null) {
                    manager.updateTask(id, task);
                    sendText(ex, "Subtask " + task.getId() + " successfully updated");
                } else {
                    sendNotFound(ex, 404, "Subtask not found");
                }
            }
        }

        public void handleDelete(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                sendNotFound(ex, 404, "Task id not provided");
            } else {
                if (manager.getTask(id) != null) {
                    manager.deleteByID(id);
                    sendText(ex, "Task " + id + " successfully deleted");
                } else {
                    sendNotFound(ex, 404, "No such task");
                }
            }
        }
    }

    static class EpicsHandler extends BaseHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            System.out.println("Started handling /epics request from client.");

            String method = ex.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePreflight(ex);
                return;
            }
            switch (method) {
                case "GET" -> handleGet(ex);
                case "POST" -> handlePost(ex);
                case "DELETE" -> handleDelete(ex);
                default -> {
                    sendNotFound(ex, 405, "Nothing was found for your request");
                    return;
                }
            }
        }

        public Integer identifyId(HttpExchange ex) throws IOException {
            String p = ex.getRequestURI().getPath();
            String[] parts = p.split("/");
            if (parts.length >= 3) {
                try {
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "Invalid request");
                }
            }
            return null;
        }

        public boolean identifySubtasks(HttpExchange ex) throws IOException {
            String p = ex.getRequestURI().getPath();
            String[] parts = p.split("/");
            if (parts.length >= 4) {
                try {
                    if (parts[4].equals("subtasks")) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "Invalid request");
                }
            }
            return false;
        }

        public void handleGet(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            boolean subtasks = identifySubtasks(ex);
            if (id == null && !subtasks) {
                String json = gson.toJson(manager.getAllEpics());
                sendResponse(ex, 200, json);
            } else if (!subtasks) {
                if (manager.getTask(id) instanceof Epic t) {
                    String json = gson.toJson(t);
                    sendResponse(ex, 200, json);
                } else {
                    sendNotFound(ex, 404, "Epic with such id not found");
                }
            } else {
                if (manager.getTask(id) instanceof Epic t) {
                    String json = gson.toJson(t.subtasks.values());
                    sendResponse(ex, 200, json);
                } else {
                    sendNotFound(ex, 404, "Epic with such id not found");
                }
            }
        }

        public void handlePost(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                InputStream is = ex.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    Epic task = gson.fromJson(body, Epic.class);
                    task.setId(-1);
                    manager.addTask(task);
                    sendResponse(ex, 201, "Epic " + task.getId() + " successfully added");
                } catch (Exception e) {
                    sendResponse(ex, 400, e.getMessage());
                    return;
                }
            } else {
                sendNotFound(ex, 500, "Invalid command");
            }
        }

        public void handleDelete(HttpExchange ex) throws IOException {
            Integer id = identifyId(ex);
            if (id == null) {
                sendNotFound(ex, 404, "Task id not provided");
            } else {
                if (manager.getTask(id) != null) {
                    manager.deleteByID(id);
                    sendText(ex, "Task " + id + " successfully deleted");
                } else {
                    sendNotFound(ex, 404, "No such task");
                }
            }
        }
    }

    static class HistoryHandler extends BaseHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                handlePreflight(ex);
                return;
            }
            if (method.equals("GET")) {
                String json = gson.toJson(manager.getHistory());
                sendResponse(ex, 200, json);
            } else {
                sendNotFound(ex, 405, "Nothing was found for your request");
                return;
            }
        }
    }

    static class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            if (method.equals("GET")) {
                String json = gson.toJson(manager.getPrioritizedTasks());
                sendResponse(ex, 200, json);
            } else {
                sendNotFound(ex, 405, "Nothing was found for your request");
                return;
            }
        }
    }
}