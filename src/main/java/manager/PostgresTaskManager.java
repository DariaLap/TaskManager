package manager;

import model.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class PostgresTaskManager implements TaskManager {
    private final HistoryManager history = Managers.getDefaultHistory();

    public PostgresTaskManager() {
        // can create tables here
    }

    // ---------- helpers ----------

    private int nextId(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT nextval('tasks_id_seq')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Integer minutesOrNull(Duration d) {
        if (d == null) return null;
        long m = d.toMinutes();
        if (m > Integer.MAX_VALUE) throw new IllegalArgumentException("duration too big (minutes > Integer.MAX_VALUE)");
        return (int) m;
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String type = rs.getString("type");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        Status status = Status.valueOf(rs.getString("status"));

        Timestamp ts = rs.getTimestamp("start_time");
        LocalDateTime start = ts != null ? ts.toLocalDateTime() : null;

        Integer durMin = (Integer) rs.getObject("duration_min");
        Duration duration = (durMin != null) ? Duration.ofMinutes(durMin) : null;

        Integer epicId = (Integer) rs.getObject("epic_id");

        switch (type) {
            case "EPIC" -> {
                Epic e = new Epic(name, desc);
                e.setId(id);
                e.setStatus(status);
                return e;
            }
            case "SUBTASK" -> {
                SubTask s = (start != null && duration != null)
                        ? new SubTask(name, desc, status, start, duration, epicId)
                        : new SubTask(name, desc, status, epicId);
                s.setId(id);
                return s;
            }
            default -> {
                Task t = (start != null && duration != null)
                        ? new Task(name, desc, status, start, duration)
                        : new Task(name, desc, status);
                t.setId(id);
                return t;
            }
        }
    }

    private Status calcEpicStatus(int epicId, Connection c) throws SQLException {
        int total = 0, newCnt = 0, doneCnt = 0;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT status FROM tasks WHERE type='SUBTASK' AND epic_id=?")) {
            ps.setInt(1, epicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total++;
                    Status s = Status.valueOf(rs.getString(1));
                    if (s == Status.NEW) newCnt++;
                    else if (s == Status.DONE) doneCnt++;
                }
            }
        }
        if (total == 0) return Status.NEW;
        if (doneCnt == total) return Status.DONE;
        if (newCnt == total)  return Status.NEW;
        return Status.IN_PROGRESS;
    }

    private void updateEpicStatus(int epicId, Connection c) throws SQLException {
        Status st = calcEpicStatus(epicId, c);
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE tasks SET status=? WHERE id=? AND type='EPIC'")) {
            ps.setString(1, st.name());
            ps.setInt(2, epicId);
            ps.executeUpdate();
        }
    }

    // ---------- TaskManager impl ----------

    @Override
    public void addTask(Task task) {
        if (!(task instanceof Epic)) checkIntersection(task);
        try (Connection c = Db.getConnection()) {
            int id = (task.getId() == null || task.getId() == -1) ? nextId(c) : task.getId();
            task.setId(id);

            String type = (task instanceof Epic) ? "EPIC" : (task instanceof SubTask ? "SUBTASK" : "TASK");
            String sql = "INSERT INTO tasks (id,type,name,description,status,start_time,duration_min,epic_id) VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setString(2, type);
                ps.setString(3, task.getName());
                ps.setString(4, task.getDescription());
                ps.setString(5, task.getStatus().name());
                if (task.getStartTime() != null) ps.setTimestamp(6, Timestamp.valueOf(task.getStartTime()));
                else ps.setNull(6, Types.TIMESTAMP);
                Integer mins = minutesOrNull(task.getDuration());
                if (mins != null) ps.setInt(7, mins); else ps.setNull(7, Types.INTEGER);
                if (task instanceof SubTask s) ps.setInt(8, s.getEpicId()); else ps.setNull(8, Types.INTEGER);
                ps.executeUpdate();
            }
            if (task instanceof SubTask s) updateEpicStatus(s.getEpicId(), c);
        } catch (SQLException e) {
            throw new RuntimeException("DB addTask failed", e);
        }
    }

    @Override
    public List<SubTask> getSubTasks() {
        String sql = "SELECT * FROM tasks WHERE type='SUBTASK' ORDER BY id";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<SubTask> out = new ArrayList<>();
            while (rs.next()) out.add((SubTask) mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Task getTask(int id) {
        String sql = "SELECT * FROM tasks WHERE id=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Task t = mapRow(rs);
                history.add(t);
                return t;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateTask(int id, Task task) {
        try (Connection c = Db.getConnection()) {
            String existingType;
            try (PreparedStatement find = c.prepareStatement("SELECT type FROM tasks WHERE id=?")) {
                find.setInt(1, id);
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Update failed! Task with such ID not found."); return;
                    }
                    existingType = rs.getString(1);
                }
            }
            String newType = (task instanceof Epic) ? "EPIC" : (task instanceof SubTask ? "SUBTASK" : "TASK");
            if (!existingType.equals(newType)) {
                System.out.println("Error: Cannot update " + existingType + " to " + newType);
                return;
            }

            String sql = "UPDATE tasks SET name=?, description=?, status=?, start_time=?, duration_min=?, epic_id=? WHERE id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, task.getName());
                ps.setString(2, task.getDescription());
                ps.setString(3, task.getStatus().name());
                if (task.getStartTime() != null) ps.setTimestamp(4, Timestamp.valueOf(task.getStartTime()));
                else ps.setNull(4, Types.TIMESTAMP);
                Integer mins = minutesOrNull(task.getDuration());
                if (mins != null) ps.setInt(5, mins); else ps.setNull(5, Types.INTEGER);
                if (task instanceof SubTask s) ps.setInt(6, s.getEpicId()); else ps.setNull(6, Types.INTEGER);
                ps.setInt(7, id);
                ps.executeUpdate();
            }

            if (history.getHistory().stream().anyMatch(t -> Objects.equals(t.getId(), id))) {
                Task fresh = getTask(id);
                if (fresh != null) history.add(fresh);
            }

            if ("SUBTASK".equals(existingType)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT epic_id FROM tasks WHERE id=?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Integer epicId = (Integer) rs.getObject(1);
                            if (epicId != null) updateEpicStatus(epicId, c);
                        }
                    }
                }
            }
            System.out.println("Updated successfully!");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByID(int id) {
        try (Connection c = Db.getConnection()) {
            String type = null; Integer epicId = null;
            try (PreparedStatement f = c.prepareStatement("SELECT type, epic_id FROM tasks WHERE id=?")) {
                f.setInt(1, id);
                try (ResultSet rs = f.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Delete failed! Task with such ID not found."); return;
                    }
                    type = rs.getString(1);
                    epicId = (Integer) rs.getObject(2);
                }
            }

            if ("EPIC".equals(type)) {
                try (PreparedStatement s = c.prepareStatement("SELECT id FROM tasks WHERE type='SUBTASK' AND epic_id=?")) {
                    s.setInt(1, id);
                    try (ResultSet rs = s.executeQuery()) {
                        while (rs.next()) history.remove(rs.getInt(1));
                    }
                }
                try (PreparedStatement d = c.prepareStatement("DELETE FROM tasks WHERE id=?")) {
                    d.setInt(1, id);
                    d.executeUpdate();
                }
                history.remove(id);
                System.out.println("Epic and its subtasks deleted.");
                return;
            }

            try (PreparedStatement d = c.prepareStatement("DELETE FROM tasks WHERE id=?")) {
                d.setInt(1, id);
                d.executeUpdate();
            }
            history.remove(id);

            if ("SUBTASK".equals(type) && epicId != null) updateEpicStatus(epicId, c);

            System.out.println("Deleted successfully!");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getAllSubTasks(int epicId) {
        String sql = "SELECT * FROM tasks WHERE type='SUBTASK' AND epic_id=? ORDER BY id";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, epicId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("Epic contains the following subtasks: ");
                while (rs.next()) System.out.println(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStatus(int id, Status status) {
        try (Connection c = Db.getConnection()) {
            String type = null; Integer epicId = null;
            try (PreparedStatement f = c.prepareStatement("SELECT type, epic_id FROM tasks WHERE id=?")) {
                f.setInt(1, id);
                try (ResultSet rs = f.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Task with ID " + id + " not found."); return;
                    }
                    type = rs.getString(1);
                    epicId = (Integer) rs.getObject(2);
                }
            }
            if ("EPIC".equals(type)) {
                System.out.println("Epic status cannot be changed manually — it is calculated automatically.");
                return;
            }
            try (PreparedStatement u = c.prepareStatement("UPDATE tasks SET status=? WHERE id=?")) {
                u.setString(1, status.name());
                u.setInt(2, id);
                u.executeUpdate();
            }
            if ("SUBTASK".equals(type) && epicId != null) updateEpicStatus(epicId, c);
            System.out.println("Task ID " + id +  " status successfully updated.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAllTasks() {
        try (Connection c = Db.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE tasks CASCADE");
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void printAllTasks() {
        String sql = "SELECT * FROM tasks ORDER BY id";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Task t = mapRow(rs);
                System.out.println(t.getId() + " " + t);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void printHistory() {
        for (Task t : history.getHistory()) System.out.println(t);
    }

    @Override
    public HashMap<Integer, Task> getAllTasks() {
        String sql = "SELECT * FROM tasks ORDER BY id";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            HashMap<Integer, Task> map = new HashMap<>();
            while (rs.next()) {
                Task t = mapRow(rs);
                map.put(t.getId(), t);
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HashMap<Integer, Task> getAllEpics() {
        String epicsSql = "SELECT * FROM tasks WHERE type='EPIC' ORDER BY id";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(epicsSql)) {
            HashMap<Integer, Task> map = new HashMap<>();
            while (rs.next()) {
                Epic e = (Epic) mapRow(rs);
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM tasks WHERE type='SUBTASK' AND epic_id=? ORDER BY id")) {
                    ps.setInt(1, e.getId());
                    try (ResultSet srs = ps.executeQuery()) {
                        while (srs.next()) e.addSubtask(((SubTask) mapRow(srs)).getId(), (SubTask) mapRow(srs));
                    }
                }
                e.setStatus(calcEpicStatus(e));
                map.put(e.getId(), e);
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Task> getHistory() {
        return history.getHistory(); }

    @Override
    public TreeSet<Task> getPrioritizedTasks() {
        String sql = "SELECT * FROM tasks WHERE type <> 'EPIC' AND start_time IS NOT NULL ORDER BY start_time";
        try (Connection c = Db.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            TreeSet<Task> set = new TreeSet<>(Comparator.comparing(Task::getStartTime));
            while (rs.next()) set.add(mapRow(rs));
            return set;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Status calcEpicStatus(Epic e) {
        if (e.subtasks == null || e.subtasks.isEmpty()) return Status.NEW;
        int total = e.subtasks.size();
        long newCnt  = e.subtasks.values().stream().filter(t -> t.getStatus()==Status.NEW).count();
        long doneCnt = e.subtasks.values().stream().filter(t -> t.getStatus()==Status.DONE).count();
        if (doneCnt == total) return Status.DONE;
        if (newCnt  == total) return Status.NEW;
        return Status.IN_PROGRESS;
    }

    public boolean checkIntersection(Task task) {
        if (task.getStartTime() == null || task.getEndTime() == null) return false;
        String sql = """
            SELECT 1 FROM tasks
             WHERE type <> 'EPIC'
               AND start_time IS NOT NULL
               AND duration_min IS NOT NULL
               AND id <> COALESCE(?, -1)
               AND start_time < ?
               AND (start_time + (duration_min || ' minutes')::interval) > ?
             LIMIT 1
            """;
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (task.getId()!=null) ps.setInt(1, task.getId()); else ps.setNull(1, Types.INTEGER);
            ps.setTimestamp(2, Timestamp.valueOf(task.getEndTime()));
            ps.setTimestamp(3, Timestamp.valueOf(task.getStartTime()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("Task overlaps in time with another task");
                return false;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}