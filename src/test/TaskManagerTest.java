package test;

import manager.Managers;
import manager.TaskManager;
import model.Status;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class TaskManagerTest<T extends TaskManager> {

    TaskManager manager;
    LocalDateTime defaultTime;
    LocalDateTime defaultTime2;
    @BeforeEach
    void beforeEach() {
        manager = Managers.getInMemory();
        defaultTime = LocalDateTime.now();
        defaultTime2 = LocalDateTime.now().plusDays(1);
    }
    @Test
    void shouldAddTaskCorrectly() {
        Task task = new Task("Task", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10));
        manager.addTask(task);

        assertEquals(task, manager.getTask(task.getId()));
    }

    @Test
    void checkTimeIntersections() {
        Task task = new Task("Task", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10));
        manager.addTask(task);

        Task task2 = new Task("Task", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10));
        //не дает добавить с таким же временем
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addTask(task2);
        });

    }


}