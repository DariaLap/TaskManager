package manager;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class FileBackedTaskManager extends InMemoryTaskManager {
    File file;
    static boolean isLoading = false;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    public static FileBackedTaskManager loadFromFile(File file) throws IOException {
        isLoading = true;
        try {
            if (file.createNewFile()) {
                System.out.println("File not found. New one created" + file.getName());
            }
        } catch (IOException e) {
            System.out.println("Cannot find the file: " + e.getMessage());
        }


        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.ready()) {
                String line = br.readLine();
                Task task = fromString(line);
                manager.addTask(task);
            }
        }
        isLoading = false;
        return manager;
    }

    @Override
    public void addTask(Task task) {
        super.addTask(task);
        if (!isLoading) {
            save();
        }
    }

    @Override
    public void updateTask(int id, Task task) {
        super.updateTask(id, task);
        save();
    }

    @Override
    public void updateStatus(int id, Status status) {
        super.updateStatus(id, status);
        save();
    }

    @Override
    public void deleteByID(int id) {
        super.deleteByID(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Task task : getAllTasks().values()) {
                writer.write(task.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Error when saving to file");
        }
    }

    public static Task fromString(String value) {

        String[] parts = value.split(",");
        if (parts[1].equals(TaskType.TASK.toString())) {
            Task task = new Task(parts[2], parts[4], Status.valueOf(parts[3]), LocalDateTime.parse(parts[5]), Duration.ofMinutes(Integer.parseInt(parts[6])));
            // TO DO try / catch
            task.setId(Integer.parseInt(parts[0]));
            return task;
        } else if (parts[1].equals(TaskType.SUBTASK.toString())) {
            SubTask subtask = new SubTask(parts[2], parts[4], Status.valueOf(parts[3]),LocalDateTime.parse(parts[6]), Duration.ofMinutes(Integer.parseInt(parts[7])), Integer.parseInt(parts[5]));
            // TO DO try / catch
            subtask.setId(Integer.parseInt(parts[0]));
            return subtask;
        } else if (parts[1].equals(TaskType.EPIC.toString())) {
            Epic epic = new Epic(parts[2], parts[4], Status.valueOf(parts[3]), new HashMap<>());
            // TO DO try / catch
            epic.setId(Integer.parseInt(parts[0]));
            return epic;
        }
        return null;
    }

    public static void main(String[] args) throws IOException {

        File file = new File("demo_java_kanban.csv");
        TaskManager manager = FileBackedTaskManager.loadFromFile(file);


        Task task = new Task("Task1", "Desc1", Status.NEW);
        manager.addTask(task);

        Epic epic = new Epic("Epic1", "Epic Desc");
        manager.addTask(epic);

        SubTask sub1 = new SubTask("Subtask1", "Sub 1", Status.NEW, epic.getId());
        manager.addTask(sub1);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        System.out.println("Loaded tasks:");
        loaded.printAllTasks();
    }
}