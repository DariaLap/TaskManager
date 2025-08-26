package test;

import manager.FileBackedTaskManager;
import manager.Managers;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends test.TaskManagerTest {

    @Test
    void shouldSaveAndLoadTasksCorrectly() throws IOException {

        File tempFile = File.createTempFile("test", ".csv");


        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        Task task = new Task("Task1", "Desc1", Status.NEW, defaultTime, Duration.ofMinutes(10));
        manager.addTask(task);

        Epic epic = new Epic("Epic1", "Epic desc");
        manager.addTask(epic);

        SubTask subtask = new SubTask("Subtask1", "Subdesc", Status.IN_PROGRESS, defaultTime2, Duration.ofMinutes(10), epic.getId());
        manager.addTask(subtask);


        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);


        Map<Integer, Task> tasks = loaded.getAllTasks();

        assertEquals(3, tasks.size(), "Должно быть загружено 3 задачи");

        assertEquals("Task1", tasks.get(task.getId()).getName());
        assertEquals("Epic1", tasks.get(epic.getId()).getName());
        assertEquals("Subtask1", tasks.get(subtask.getId()).getName());
    }

    @Test
    void saveAndLoadEmptyManager() throws IOException {

        File tempFile = File.createTempFile("test", ".csv");
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(tempFile);
        assertTrue(loadedManager.getAllTasks().isEmpty(), "После создания нового файла список задач должен быть пуст");

        loadedManager.deleteAllTasks();//forced save
        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(tempFile);
        assertTrue(manager.getAllTasks().isEmpty(), "После загрузки из пустого файла список задач должен быть пуст");
    }

    @Test
    void shouldNotThrowWhenFileDoesNotExist() {
        File tempFile = new File("temp_test_file.csv");

        if (tempFile.exists()) {
            tempFile.delete();
        }

        assertDoesNotThrow(() -> FileBackedTaskManager.loadFromFile(tempFile));

        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void shouldThrowIOExceptionWhenFileIsInvalid() {
        File file = new File("");

        assertThrows(IOException.class, () -> FileBackedTaskManager.loadFromFile(file));
    }
}