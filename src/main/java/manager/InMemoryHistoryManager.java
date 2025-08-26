package manager;

import model.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    private final CustomLinkedList history = new CustomLinkedList();
    private final Map<Integer, CustomLinkedList.Node<Task>> map = new HashMap<>();

    @Override
    public void add(Task task) {
        if (task == null) return;

        remove(task.getId());
        CustomLinkedList.Node<Task> node = history.addLast(task);
        map.put(task.getId(), node);

    }

    @Override
    public void remove(int id) {
        history.remove(map.get(id));
    }

    @Override
    public List<Task> getHistory() {
        return history.toList();
    }
}