package test;

import model.Epic;
import model.Status;
import model.SubTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryTaskManagerTest extends TaskManagerTest {

    @Test
    void subtasksHaveEpic() {
        Epic manualTask = new Epic("Manual", "desc", Status.NEW, new HashMap<>());
        manager.addTask(manualTask);

        SubTask manualSubtask = new SubTask("Manual", "desc", Status.NEW, defaultTime, Duration.ofMinutes(10),0);
        manualTask.addSubtask(manualSubtask.getId(), manualSubtask);
        manager.addTask(manualSubtask);


        assertEquals(manualTask.getId(), manualSubtask.getEpicId());
    }
}