package model;

import java.time.LocalDateTime;
import java.util.HashMap;

public class Epic extends Task {

    public HashMap<Integer, Task> subtasks = new HashMap<>();
    protected LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
        this.setStatus(Status.NEW);
        this.subtasks = new HashMap<>();
    }

    public Epic(String name,String description,Status status,HashMap<Integer,Task> subtasks) {
        super(name, description);
        this.setStatus(status);
        this.subtasks = (subtasks != null) ? subtasks : new HashMap<>();
    }

    public void addSubtask(int subtaskId, Task task) {
        subtasks.put(subtaskId, task);
        getEndTime();
    }

    @Override
    public String toString() {
        return this.getId() + ",EPIC," + this.getName() + "," + this.getStatus() +
                "," + this.getDescription() + ",";
    }

    public void removeSubTask(int id) {
        subtasks.remove(id);
        if (!subtasks.isEmpty()) {
            getEndTime();
        }
    }

    @Override
    public LocalDateTime getEndTime() {
        this.startTime = null;
        this.endTime = null;

        for (Task subtask : subtasks.values()) {
            if (subtask.startTime != null && subtask.duration != null) {
                if (startTime == null || subtask.startTime.isBefore(startTime)) {
                    this.startTime = subtask.startTime;
                }
                if (endTime == null) {
                    endTime = subtask.startTime.plus(subtask.duration);
                }
                if (subtask.startTime.plus(subtask.duration).isAfter(endTime)) {
                    endTime = subtask.startTime.plus(subtask.duration);
                }
            }
        }
        return endTime;
    }

}