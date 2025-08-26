package test;

import manager.Managers;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
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
    void twoTasksAreEqualWithSameIds() {

        Task task1 = new Task("qwe","qew", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("qwe","qew", Status.NEW);
        task2.setId(1);

        assertEquals(task1, task2);

    }

    @Test
    void twoInheritedTasksAreEqualWithSameIds(){
        Task task1 = new Epic("qwe","qew", Status.NEW, null);
        task1.setId(1);
        Task task2 = new Epic("qwe","qew", Status.NEW, null);
        task2.setId(1);

        assertEquals(task1, task2);
    }



    @Test
    void addNewTask() {
        Task task = new Task("Test addNewTask", "Test addNewTask description", Status.NEW, defaultTime, Duration.ofMinutes(10));
        manager.addTask(task);
        final int taskId = task.getId();

        final Task savedTask = manager.getTask(taskId);

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");



        HashMap<Integer, Task> tasks = manager.getAllTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertEquals(1, tasks.size(), "Неверное количество задач.");
        assertEquals(task, tasks.get(0), "Задачи не совпадают.");
    }

    @Test
    void firstAddedTaskHasZeroId() {

        Task autoTask = new Task("Auto", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10));
        manager.addTask(autoTask);

        Assertions.assertNotNull(manager.getTask(autoTask.getId()), "Автоматически добавленная задача должна быть в менеджере");
        assertEquals(0, autoTask.getId(), "Первый авто-сгенерированный ID должен быть 0");
    }

    @Test
    void subtasksDeletedIfEpicDeleted(){
        Epic manualTask = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualTask);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10),0);
        manualTask.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);

        manager.deleteByID(0);

        assertTrue(manager.getAllTasks().isEmpty(), "Все задачи должны быть удалены");
    }

}