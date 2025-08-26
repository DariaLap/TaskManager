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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EpicTest {

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
    void EpicWithNewSubtasks() {
        Epic manualEpic = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualEpic);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10),  0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);

        SubTask manualSubtask2 = new SubTask("Manual", "desc", Status.NEW, defaultTime2, Duration.ofMinutes(10),0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask2);

        assertEquals(manualEpic.getStatus(), Status.NEW);
    }

    @Test
    void EpicWithDoneSubtasks() {
        Epic manualEpic = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualEpic);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.DONE, defaultTime, Duration.ofMinutes(10),  0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);

        SubTask manualSubtask2 = new SubTask("Manual", "desc", Status.DONE, defaultTime2, Duration.ofMinutes(10),0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask2);

        assertEquals(manualEpic.getStatus(), Status.DONE);
    }

    @Test
    void EpicWithDoneAndNewSubtasks() {
        Epic manualEpic = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualEpic);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.DONE, defaultTime, Duration.ofMinutes(10),  0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);

        SubTask manualSubtask2 = new SubTask("Manual", "desc", Status.NEW, defaultTime2, Duration.ofMinutes(10),0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask2);

        assertEquals(manualEpic.getStatus(), Status.IN_PROGRESS);
    }

    @Test
    void EpicWithInProgressSubtasks() {
        Epic manualEpic = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualEpic);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.IN_PROGRESS, defaultTime, Duration.ofMinutes(10),  0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);

        SubTask manualSubtask2 = new SubTask("Manual", "desc", Status.IN_PROGRESS, defaultTime2, Duration.ofMinutes(10),0);
        manualEpic.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask2);

        assertEquals(manualEpic.getStatus(), Status.IN_PROGRESS);
    }

}