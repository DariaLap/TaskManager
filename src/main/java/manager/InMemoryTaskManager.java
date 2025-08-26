package manager;

import model.Epic;
import model.Status;
import model.SubTask;
import model.Task;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    private int counter = 0;
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    HashMap<Integer, Task> tasks = new HashMap<>();
    TreeSet<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime));

    public HashMap<Integer, Task> getAllTasks() {
        return tasks;
    }

    public HashMap<Integer, Task> getAllEpics() {
        return tasks.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Epic)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (Epic) entry.getValue(),
                        (e1, e2) -> e1, // in case of duplicate keys
                        HashMap::new
                ));
    }

    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public TreeSet<Task> getPrioritizedTasks() {
        return prioritizedTasks;
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    @Override
    public Task getTask(int id) {
        historyManager.add(tasks.get(id));
        return tasks.get(id);
    }

    @Override
    public void addTask(Task task) {

        if (!(task instanceof Epic)) {
            checkIntersection(task);
        }

        if (task.getId() == null || task.getId() == -1) {
            task.setId(counter);
        }

        if (task instanceof SubTask subtask) {
            int epicId = subtask.getEpicId();
            if (isValidEpicId(epicId)) {
                Epic epic = (Epic) tasks.get(epicId);
                epic.addSubtask(subtask.getId(), subtask);
            } else {
                System.out.println("Epic with this ID does not exist");
                return;
            }
        }
        counter++;
        tasks.put(task.getId(), task);
        if (task instanceof SubTask subtask) {
            updateEpicStatus(subtask.getEpicId());
        }
        if (!(task instanceof Epic)) {
            prioritizedTasks.add(task);
        }
    }

    @Override
    public List<SubTask> getSubTasks() {
        return tasks.values().stream()
                .filter(task -> task instanceof SubTask)
                .map(task -> (SubTask) task)
                .toList();
    }

    @Override
    public void updateTask(int id, Task task) {
        if (tasks.get(id).getClass() != task.getClass()) {
            System.out.println("Error: Can't update " + tasks.get(id).getClass().getSimpleName() +
                    " to " + task.getClass().getSimpleName());
            return;
        }
        for (int element : tasks.keySet()) {
            if (element == id) {
                Task oldTask = tasks.get(element);
                task.setId(id);
                tasks.replace(element, oldTask, task);
                System.out.println("Successfully updated!");
                // add to history
                if (historyManager.getHistory().contains(task)) {
                    historyManager.add(task);
                }
                // update in prioritizedTasks
                if (!(task instanceof Epic)) {
                    prioritizedTasks.remove(oldTask);
                    prioritizedTasks.add(task);
                }
                return;
            }
        }
        System.out.println("Update failed!");
    }

    @Override
    public void deleteByID(int id) {
        Task task = tasks.get(id);

        if (task == null) {
            System.out.println("Delete failed! No task with this ID");
            return;
        }

        if (task instanceof Epic epic) {
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t instanceof SubTask subtask && subtask.getEpicId() == epic.getId()) {
                    toRemove.add(subtask.getId());
                }
            }
            for (int subId : toRemove) {
                historyManager.remove(subId);
                tasks.remove(subId);
            }
            historyManager.remove(id);
            tasks.remove(id);
            System.out.println("Epic removed along with its " + toRemove.size() + " subtask(s).");
            return;
        }

        if (task instanceof SubTask subtask) {
            Epic epic = (Epic) tasks.get(subtask.getEpicId());
            epic.removeSubTask(subtask.getId());
            historyManager.remove(subtask.getId());
            tasks.remove(subtask.getId());
            System.out.println("Successfully deleted!");
            return;
        }
        prioritizedTasks.remove(task);
        historyManager.remove(id);
        tasks.remove(id);
        System.out.println("Successfully deleted!");
    }

    private boolean isValidEpicId(int id) {
        Task task = tasks.get(id);
        return task instanceof Epic;
    }

    @Override
    public void getAllSubTasks(int epicId) {
        Task task = tasks.get(epicId);
        if (task instanceof Epic epic) {
            System.out.println("Epic contains the following subtasks: ");
            epic.subtasks.values().stream()
                    .map(Object::toString)
                    .forEach(System.out::println);
        } else {
            System.out.println("Not an Epic");
        }
    }

    @Override
    public void updateStatus(int id, Status status) {
        Task task = tasks.get(id);

        if (task == null) {
            System.out.println("Task with ID " + id + " not found.");
            return;
        }

        if (task instanceof Epic) {
            System.out.println("Epic status cannot be changed manually — it is calculated automatically.");
            return;
        }

        task.setStatus(status);

        System.out.println("Task ID " + id + " status successfully updated.");

        if (task instanceof SubTask subtask) {
            int epicId = subtask.getEpicId();
            updateEpicStatus(epicId);
        }
    }

    private void updateEpicStatus(int epicId) {
        Epic epic = (Epic) tasks.get(epicId);
        int newCount = 0;
        int doneCount = 0;
        if (epic.subtasks == null) {
            epic.setStatus(Status.NEW);
        }

        for (Task element : epic.subtasks.values()) {
            if (element.getStatus() == Status.NEW) {
                newCount++;
            } else if (element.getStatus() == Status.DONE) {
                doneCount++;
            }
        }

        if (newCount == epic.subtasks.size()) {
            epic.setStatus(Status.NEW);
            System.out.println("Epic " + epicId + " status was updated to NEW");
        } else if (doneCount == epic.subtasks.size()) {
            epic.setStatus(Status.DONE);
            System.out.println("Epic " + epicId + " status was updated to DONE");
        } else {
            epic.setStatus(Status.IN_PROGRESS);
            System.out.println("Epic " + epicId + " status was updated to IN_PROGRESS");
        }
    }

    public void printAllTasks() {
        for (int name : tasks.keySet()) {
            String value = tasks.get(name).toString();
            System.out.println(name + " " + value);
        }
    }

    @Override
    public void printHistory() {
        // currently, history is updated only if the user explicitly viewed a task (e.g., through case 7)
        List<Task> history = historyManager.getHistory();
        for (Task name : history) {
            System.out.println(name);
        }
    }

    public boolean checkIntersection(Task task) {
        boolean hasIntersection = prioritizedTasks.stream()
                .anyMatch(oldTask ->
                        task.getStartTime() != null &&
                                task.getEndTime() != null &&
                                oldTask.getStartTime() != null &&
                                oldTask.getEndTime() != null &&
                                // intersection condition
                                task.getStartTime().isBefore(oldTask.getEndTime()) &&
                                task.getEndTime().isAfter(oldTask.getStartTime())
                );

        if (hasIntersection) {
            throw new IllegalArgumentException("Task overlaps in time with another task");
        }

        return hasIntersection;
    }
}