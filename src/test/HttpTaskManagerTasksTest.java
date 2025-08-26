package test;

import com.google.gson.Gson;
import http.HttpTaskServer;
import manager.InMemoryTaskManager;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HttpTaskManagerTasksTest {

    // создаём экземпляр InMemoryTaskManager
    TaskManager manager = new InMemoryTaskManager();
    // передаём его в качестве аргумента в конструктор HttpTaskServer
    HttpTaskServer taskServer = new HttpTaskServer(manager);
    Gson gson = HttpTaskServer.getGson();

    public HttpTaskManagerTasksTest() throws IOException {
    }

    @BeforeEach
    public void setUp() {
        manager.deleteAllTasks();
        taskServer.start();
    }

    @AfterEach
    public void shutDown() {
        taskServer.stop();
    }

    @Test
    public void testAddTask() throws IOException, InterruptedException {
        // создаём задачу
        Task task = new Task("Test 2", "Testing task 2",
                Status.NEW, LocalDateTime.now(), Duration.ofMinutes(5));
        // конвертируем её в JSON
        String taskJson = gson.toJson(task);

        // создаём HTTP-клиент и запрос
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/tasks");
        HttpRequest request = HttpRequest.newBuilder().uri(url).POST(HttpRequest.BodyPublishers.ofString(taskJson)).build();

        // вызываем рест, отвечающий за создание задач
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // проверяем код ответа
        assertEquals(201, response.statusCode());

        // проверяем, что создалась одна задача с корректным именем
        HashMap<Integer, Task> tasksFromManager = manager.getAllTasks();

        assertNotNull(tasksFromManager, "Задачи не возвращаются");
        assertEquals(1, tasksFromManager.size(), "Некорректное количество задач");
        assertEquals("Test 2", tasksFromManager.get(0).getName(), "Некорректное имя задачи");
    }

    @Test
    public void PostEpics() throws IOException, InterruptedException {
        // создаём задачу
        Epic epic = new Epic("EpicName", "Epic Description");
        // конвертируем её в JSON
        String taskJson = gson.toJson(epic);

        // создаём HTTP-клиент и запрос
        HttpClient client = HttpClient.newHttpClient();
        URI url = URI.create("http://localhost:8080/epics");
        HttpRequest request = HttpRequest.newBuilder().uri(url).POST(HttpRequest.BodyPublishers.ofString(taskJson)).build();

        // вызываем рест, отвечающий за создание задач
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        // проверяем, что создалась одна задача с корректным именем
        HashMap<Integer, Task> tasksFromManager = manager.getAllTasks();

        assertNotNull(tasksFromManager, "Задачи не возвращаются");
        assertEquals(1, tasksFromManager.size(), "Некорректное количество задач");
        assertEquals("EpicName", tasksFromManager.get(0).getName(), "Некорректное имя задачи");


    }

    @Test
    public void getHistory200andPrioritized200() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // пусть в истории что-то появится
        Task t = new Task("H", "D", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(1));
        manager.addTask(t);
        manager.getTask(t.getId());

        HttpResponse<String> hist = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/history")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, hist.statusCode());

        HttpResponse<String> prio = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/prioritized")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, prio.statusCode());
    }

    @Test
    public void deleteTask200andNotFound404() throws Exception {
        HttpClient client = HttpClient.newHttpClient();


        URI createUrl = URI.create("http://localhost:8080/tasks");
        Task t = new Task("ToDelete", "Desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(1));
        client.send(HttpRequest.newBuilder(createUrl)
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(t)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        int id = manager.getAllTasks().keySet().iterator().next();


        URI delUrl = URI.create("http://localhost:8080/tasks/" + id);
        HttpResponse<String> delResp = client.send(
                HttpRequest.newBuilder(delUrl).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, delResp.statusCode());


        HttpResponse<String> del404 = client.send(
                HttpRequest.newBuilder(delUrl).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, del404.statusCode());
    }
}