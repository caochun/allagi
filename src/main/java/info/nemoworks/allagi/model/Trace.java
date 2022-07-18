package info.nemoworks.allagi.model;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.model.EnterableState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Trace {

    Log log;

    @Getter
    private final ArrayList<Node> trace;

    public Trace() {
        log = LogFactory.getLog(this.getClass());
        trace = new ArrayList<>();
    }

    public synchronized boolean append(Set<EnterableState> configuration) {
        return this.append(configuration, ORIGIN.NORMAL);
    }

    public synchronized boolean append(Set<EnterableState> configuration, ORIGIN origin) {
        return trace.add(new Node(origin, configuration));
    }

    public Set<RecordNode> getRecordNodes() {
        return trace.stream().flatMap(node -> node.getRecordNodes().stream()).collect(Collectors.toSet());
    }


    @Data
    public static class Node {

        private Instant instant;
        private ORIGIN origin;
        private Set<RecordNode> recordNodes;
        private Set<EnterableState> configuration;

        public Node(Instant instant, ORIGIN origin, Set<RecordNode> recordNodes, Set<EnterableState> configuration) {
            this.instant = instant;
            this.origin = origin;
            this.recordNodes = recordNodes;
            this.configuration = new HashSet<>(configuration);
        }

        public Node(Instant instant, ORIGIN origin, Set<EnterableState> configuration) {
            this(instant, origin, generateRecordNodes(configuration), configuration);
        }

        public Node(ORIGIN origin, Set<EnterableState> configuration) {
            this(Instant.now(), origin, configuration);
        }

        public static Set<RecordNode> generateRecordNodes(Set<EnterableState> configuration) {
            return configuration.stream()
                    .filter(EnterableState::isAtomicState)
                    .flatMap(enterableState -> enterableState.getOnEntries().stream())
                    .flatMap(onEntry -> onEntry.getActions().stream())
                    .filter(action -> action instanceof Task)
                    .map(action -> (Task) action)
                    .map(RecordNode::new)
                    .collect(Collectors.toSet());
        }
    }


    @Data
    public static class RecordNode {

        private Task task;
        private Task taskRecord;


        public RecordNode(Task task, Task taskRecord) {
            this.task = task;
            this.taskRecord = taskRecord;
        }

        public RecordNode(Task task) {
            this(task, new Task(task));
        }
    }

    public static enum ORIGIN {
        NORMAL, GOTO, WITHDRAW
    }
}
