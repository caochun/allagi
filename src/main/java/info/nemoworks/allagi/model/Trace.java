package info.nemoworks.allagi.model;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;
import java.util.Set;

public class Trace {

    Log log;

    @Getter
    private Node head;

    public Task getHeadTask() {
        return head.getTask();
    }

    public Node getLatest(Task task) {
        Node n = head;
        while ((n != null) && (n.getTask() == task)) {
            //still only chain considered
            n = trace.predecessors(n).iterator().next();
        }
        return n;
    }

    public Node getPre(Node node) {
        return trace.predecessors(node).iterator().next();
    }

    public Node getNext(Node node) {
        return trace.successors(node).iterator().next();
    }

    public Set<EndpointPair<Node>> getEdges() {
        return trace.edges();
    }

    public Set<Node> getNodes() {
        return trace.nodes();
    }

    public Trace() {
        log = LogFactory.getLog(this.getClass());
        trace = GraphBuilder.directed().build();
    }


    public synchronized boolean append(Task task) {
        return this.append(task, ORIGIN.NORMAL);
    }

    public synchronized boolean append(Task task, ORIGIN origin) {
        Node current = new Node(task, origin);

        if (head == null){
            trace.addNode(current);
            head= current;
            return true;
        }

        if (trace.putEdge(head, current)) {
            head = current;
            return true;
        }
        return false;
    }

    public synchronized boolean deduct(Task task) {
        if (head.getTask() != task)
            return false;
        //multiple predecessors is not considered currently
        Node pre = trace.predecessors(head).iterator().next();

        if (pre == null)
            return false;

        trace.removeEdge(pre, head);
        trace.removeNode(head);
        head = pre;
        return true;
    }

//    public synchronized boolean unwind(Task task) {
//
//        if (trace.predecessors(head).stream().filter(n -> n.getTask().equals(task)).count() == 0) return false;
//        if (head.getTask().getStatus() != Task.STATUS.PENDING) return false;
//
//
//    }


    private MutableGraph<Node> trace;


    @Data
    public static class Node {

        private Instant instant;
        private Task task;
        private Task taskRecord;
        private ORIGIN origin;

        public Node(Task task) {
            this(task, ORIGIN.NORMAL);
        }

        public Node(Task task, ORIGIN origin) {
            this(Instant.now(), task, origin);
        }

        public Node(Instant instant, Task task) {
            this(instant, task, ORIGIN.NORMAL);
        }

        public Node(Instant instant, Task task, ORIGIN origin) {
            this.instant = instant;
            this.task = task;
            this.taskRecord = new Task(task);
            this.origin = origin;
        }

        public String toString() {
            return task + "@" + instant.toString();
        }


    }

    public static enum ORIGIN {
        NORMAL, GOTO, WITHDRAW
    }
}
