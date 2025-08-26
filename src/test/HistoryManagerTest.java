package test;

import manager.Managers;
import manager.TaskManager;
import model.Status;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest extends TaskManagerTest {

    @Test
    void tasksShouldAppearInHistoryAfterGetById() {
        Task task1 = new Task("Task1", "Description1", Status.NEW, defaultTime, Duration.ofMinutes(10));
        Task task2 = new Task("Task2", "Description2", Status.NEW, defaultTime2, Duration.ofMinutes(10));

        manager.addTask(task1);
        manager.addTask(task2);


        manager.getTask(task1.getId());
        manager.getTask(task2.getId());

        List<Task> history = manager.getHistory();

        assertEquals(2, history.size(), "История должна содержать 2 задачи");
        assertTrue(history.contains(task1), "История должна содержать task1");
        assertTrue(history.contains(task2), "История должна содержать task2");


        assertEquals(task1, history.get(0), "Первая задача в истории — task1");
        assertEquals(task2, history.get(1), "Вторая задача в истории — task2");
    }

    @Test
    void deletedFromHistoryAfterDeletion(){
        Task task1 = new Task("Task1", "Description1", Status.NEW, defaultTime, Duration.ofMinutes(10));
        Task task2 = new Task("Task2", "Description2", Status.NEW, defaultTime2, Duration.ofMinutes(10));

        manager.addTask(task1);
        manager.addTask(task2);

        manager.getTask(task1.getId());
        manager.getTask(task2.getId());

        manager.deleteByID(task1.getId());
        manager.deleteByID(task2.getId());

        assertTrue(manager.getHistory().isEmpty(), "Все задачи должны быть удалены");
    }

    @Test
    void emptyHistory() {
        Task task1 = new Task("Task1", "Description1", Status.NEW, defaultTime, Duration.ofMinutes(10));
        Task task2 = new Task("Task2", "Description2", Status.NEW, defaultTime2, Duration.ofMinutes(10));
        manager.addTask(task1);
        manager.addTask(task2);
        assertTrue(manager.getHistory().isEmpty(), "История должна быть пустой");
    }

    @Test
    void duplicateTaskInHistory() {

        Task task1 = new Task("Task1", "Description1", Status.NEW, defaultTime, Duration.ofMinutes(10));

        manager.addTask(task1);

        manager.getTask(task1.getId());
        manager.getTask(task1.getId());

        List<Task> history = manager.getHistory();

        assertEquals(1, history.size(), "История должна содержать 1 задачу");

    }


}