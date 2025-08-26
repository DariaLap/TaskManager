package manager;

import java.io.File;
import java.io.IOException;

public class Managers  {

    public static TaskManager getDefault()  {

        // return new InMemoryTaskManager();
        // return getFileBackedManager();
        return new PostgresTaskManager();
    }

    public static TaskManager getInMemory()  {
        return new InMemoryTaskManager();
    }

    public static TaskManager getFileBackedManager() throws IOException {
        return FileBackedTaskManager.loadFromFile(new File("java_kanban.csv"));
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}