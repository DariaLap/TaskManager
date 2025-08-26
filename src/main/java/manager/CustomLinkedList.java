package manager;

import model.Task;

import java.util.LinkedList;

public class CustomLinkedList {

    public static class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;

        public Node(T data, Node<T> next, Node<T> prev) {
            this.data = data;
            this.next = next;
            this.prev = prev;
        }
    }

    Node<Task> head;
    Node<Task> tail;

    public void remove(Node<Task> node) {

        if (node == null) return;

        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }

    public Node<Task> addLast(Task task) {
        Node<Task> newNode = new Node<>(task, null, tail);

        if (tail != null) {
            tail.next = newNode;
        } else {
            head = newNode;
        }

        tail = newNode;
        return newNode;
    }

    public LinkedList<Task> toList() {
        LinkedList<Task> result = new LinkedList<>();
        Node<Task> cur = head;
        while (cur != null) {

            result.add(cur.data);
            cur = cur.next;
        }
        return result;
    }

}