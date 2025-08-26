package manager;

import model.Status;
import model.SubTask;
import model.Task;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public interface TaskManager {

    void addTask(Task task);

    List<SubTask> getSubTasks();

    Task getTask(int id);

    void updateTask(int id, Task task);

    void deleteByID(int  id);

    void getAllSubTasks(int epicId);

    void updateStatus(int id, Status status);

    void deleteAllTasks();

    void printAllTasks();

    void printHistory();

    HashMap<Integer, Task> getAllTasks();

    HashMap<Integer, Task> getAllEpics();

    List<Task> getHistory();

    TreeSet<Task> getPrioritizedTasks();

}