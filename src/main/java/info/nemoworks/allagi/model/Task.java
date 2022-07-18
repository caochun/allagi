package info.nemoworks.allagi.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLSystemContext;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.*;
import org.apache.commons.scxml2.system.EventVariable;

import java.util.*;
import java.util.stream.Collectors;

public class Task extends Action {

    Log log;

    private boolean initialized = false;

    @Getter
    @Setter
    private String name;


    @Getter
    @Setter
    private String stateId;

    @Getter
    @Setter
    private String completeEvent;

    @Getter
    @Setter
    private STATUS status;

    @Getter
    private String taskId;

    // Every task has its own lifecycle
    // INITIALIZED (execute)-> <--(cancel) PENDING (accept)-> ACCEPTED (complete)-> COMPLETED (execute)--> PENDING...
    public static enum STATUS {
        INITIALIZED, PENDING, ACCEPTED, COMPLETED
    }


    //tasks will be initialized when the scxml reader build the machine up
    public Task() {
        log = LogFactory.getLog(this.getClass());
        this.status = STATUS.INITIALIZED;
    }

    public Task(Task task) {
        this.initialized = task.initialized;
        this.log = task.log;
        this.name = task.name;
        this.stateId = task.stateId;
        this.completeEvent = task.completeEvent;
        this.status = task.status;
        this.taskId = task.taskId;
        this.trace = task.trace;
        this.flow = task.flow;
        this.context = task.context;
    }

    public boolean cancel() {
        if (this.status != STATUS.PENDING)
            return false;

        this.status = STATUS.INITIALIZED;
//        this.trace.deduct(this);

        log.info("task " + this.getName() + " cancelled");

        return false;
    }

    public boolean complete() throws ModelException {
        if (this.status != STATUS.ACCEPTED)
            return false;

        this.status = STATUS.COMPLETED;

        this.trigger(this.completeEvent);
        log.info("task " + this.getName() + " completed");

        return true;
    }

    public void uncomplete() throws ModelException {
        // only in serial mode locally,  uncomplete is allowed
        // uncomplete is allowed only when the task is completed and nothing happens after it
        Trace.Node backNode = checkUncomplete();

        this.context.getGlobalContext().set("traceType", Trace.ORIGIN.WITHDRAW);
        StringBuilder eventName = new StringBuilder("GOTO");
        backNode.getConfiguration().forEach(e -> eventName.append("-").append(e.getId()));
        this.setStatus(STATUS.ACCEPTED);
        if (trigger(eventName.toString())) {
            log.info("task " + this.getName() + " uncompleted");
            return;
        }

        log.info("task " + this.getName() + " fail to uncomplete");
        this.context.getGlobalContext().set("traceType", null);
        throw new ModelException("task " + this.getName() + " fail to uncomplete");
    }

    private Trace.Node checkUncomplete() throws ModelException {
        if (this.status != STATUS.COMPLETED)
            throw new ModelException("task " + this.getName() + " is not completed");

        int size = trace.getTrace().size();
        if (size == 0)
            throw new ModelException("No trace to uncomplete");
        else {
            Trace.Node current = trace.getTrace().get(size - 1);
            Set<Task> currentTasks = current.getRecordNodes().stream()
                    .map(Trace.RecordNode::getTask)
                    .collect(Collectors.toSet());
            if (currentTasks.contains(this))
                return current;

            if (size == 1)
                throw new ModelException("No trace to uncomplete");

            Trace.Node last = trace.getTrace().get(size - 2);
            Set<Task> lastTasks = last.getRecordNodes().stream()
                    .map(Trace.RecordNode::getTask)
                    .collect(Collectors.toSet());
            if (!lastTasks.contains(this))
                throw new ModelException("No trace to uncomplete");

            Set<Task> newTasks = new HashSet<>(currentTasks);
            newTasks.removeAll(lastTasks);
            newTasks = newTasks.stream()
                    .filter(task -> task.getStatus() != STATUS.PENDING)
                    .collect(Collectors.toSet());
            if (newTasks.size() > 0)
                throw new ModelException("New tasks are not pending");

            return last;
        }

    }

    public void accept() throws ModelException {
        if (this.status != STATUS.PENDING)
            throw new ModelException("task " + this.getName() + " is not pending");

        this.status = STATUS.ACCEPTED;
        log.info("task " + this.getName() + " accepted");
    }

    public boolean invoke(ActionExecutionContext actionExecutionContext) {
        if (this.status != STATUS.INITIALIZED)
            return false;

        log.info("task " + this.getName() + " invoked");
        this.status = STATUS.PENDING;

        actionExecutionContext.getGlobalContext().set("traceType", Trace.ORIGIN.NORMAL);
        return true;

    }

    public void jump(Trace.Node node) throws ModelException {
        if (!this.trace.getTrace().contains(node))
            throw new ModelException("task " + this.getName() + " is not in the trace");

        this.context.getGlobalContext().set("traceType", Trace.ORIGIN.GOTO);
        StringBuilder eventName = new StringBuilder("GOTO");
        node.getConfiguration().forEach(e -> eventName.append("-").append(e.getId()));
        this.status = STATUS.COMPLETED;
        if (trigger(eventName.toString())) {
            log.info("jump to node when" + node.getInstant());
            return;
        }

        log.info("task " + this.getName() + " fail to jump");
        this.context.getGlobalContext().set("traceType", null);
        throw new ModelException("task " + this.getName() + " fail to jump");
    }

    public void jump(Task... tasks) throws ModelException {
        ArrayList<Trace.Node> nodes = this.trace.getTrace();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Set<Task> jumpTasks = nodes.get(i).getRecordNodes().stream()
                    .map(Trace.RecordNode::getTask)
                    .collect(Collectors.toSet());
            Set<Task> expectTasks = Arrays.stream(tasks)
                    .collect(Collectors.toSet());
            if (jumpTasks.size() == expectTasks.size() && jumpTasks.containsAll(expectTasks)) {
                this.jump(nodes.get(i));
                return;
            }
        }
        throw new ModelException("task " + this.getName() + " fail to jump");
    }

    private Trace trace = null;

    private Flow flow = null;

    private ActionExecutionContext context = null;

    @Override
    public void execute(ActionExecutionContext actionExecutionContext) throws ModelException {

        if (!initialized) {
            initialization(actionExecutionContext);
        }

        this.context = actionExecutionContext;
        this.taskId = UUID.randomUUID().toString();
        log.info("this :" + taskId);

        EventVariable eventVariable = (EventVariable) actionExecutionContext.getGlobalContext().get(SCXMLSystemContext.EVENT_KEY);
        if (eventVariable != null)
            log.info("task " + this.getName() + " executing, triggered by event " + eventVariable.getName());

        if (this.invoke(actionExecutionContext)) {
            log.info("task " + this.getName() + " executed, from INITIALIZED to PENDING");
        }

    }

    private void initialization(ActionExecutionContext actionExecutionContext) throws ModelException {

        if (!(this.getParent() instanceof OnEntry)) {
            throw new ModelException("task " + this.getName() + " must be a child of onEntry");
        }

        EnterableState parentState = this.getParentEnterableState();

        if (!(parentState instanceof State) || !parentState.isAtomicState()) {
            throw new ModelException("task " + this.getName() + " must in an atomic state");
        }

        Set<Task> taskSet = ((State) parentState).getOnEntries().stream()
                .flatMap(onEntry -> onEntry.getActions().stream()
                        .filter(action -> action instanceof Task)
                        .map(action -> (Task) action))
                .collect(Collectors.toSet());

        if (taskSet.size() > 1) {
            throw new ModelException("task " + this.getName() + " in a state which has more than one task");
        }

        if (trace == null) {
            Object t = actionExecutionContext.getGlobalContext().get("trace");
            if ((!(t instanceof Trace))) {
                throw new ModelException("no trace in executionContext");
            }
            this.trace = (Trace) t;
        }

        if (flow == null) {
            Object f = actionExecutionContext.getGlobalContext().get("flow");
            if ((!(f instanceof Flow))) {
                throw new ModelException("no flow in executionContext");
            }
            this.flow = (Flow) f;
        }

        initialized = true;
    }


    public boolean trigger(String event) throws ModelException {
        TriggerEvent evt = new TriggerEvent(event, TriggerEvent.SIGNAL_EVENT, this.taskId);
        flow.getEngine().triggerEvent(evt);
        log.info("Trigger event " + event + " for task " + this.getName());
        return true;
    }

}
