package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Task {

    protected String name;
    protected String description;
    protected Integer id = -1;
    protected Status status;
    protected Duration duration;
    protected LocalDateTime startTime;


    public Task(String name, String description) {

        this.name = name;
        this.description = description;

    }

    public Task(String name, String description, Status status) {

        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(String name, String description, Status status, LocalDateTime startTime, Duration duration) {

        this.name = name;
        this.description = description;
        this.status = status;
        this.startTime = startTime;
        this.duration = duration;
    }

    public void setId(int id) {

        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(int minutes) {
        this.duration = Duration.ofMinutes(minutes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return this.getId() + ",TASK," + this.getName() + "," + this.getStatus() +
                "," + this.getDescription() + "," + this.getStartTime() + "," + this.duration.toMinutes();
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getEndTime() {
        return startTime.plus(duration);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}