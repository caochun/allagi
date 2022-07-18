package info.nemoworks.allagi.model;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLSystemContext;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.*;
import org.apache.commons.scxml2.system.EventVariable;

import java.util.Set;
import java.util.UUID;
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
    private STATUS status;

    @Getter
    private String taskId;

    private String predecessorId;

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
        this.log = task.log;
        this.name = task.name;
        this.stateId = task.stateId;
        this.completeEvent = task.completeEvent;
        this.status = task.status;
        this.taskId = task.taskId;
        this.predecessorId = task.predecessorId;
        this.trace = task.trace;
        this.flow = task.flow;
    }

    public boolean equalTaskDefinition(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equal(log, task.log) && Objects.equal(name, task.name) && Objects.equal(stateId, task.stateId) && Objects.equal(completeEvent, task.completeEvent) && status == task.status && Objects.equal(predecessorId, task.predecessorId) && Objects.equal(trace, task.trace) && Objects.equal(flow, task.flow);
    }

    public boolean cancel() {
        if (this.status != STATUS.PENDING)
            return false;

        this.status = STATUS.INITIALIZED;
        this.trace.deduct(this);

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

    public boolean uncomplete() throws ModelException {
        if (trace.getHeadTask().getStatus() != STATUS.PENDING)
            return false;

        if (trace.getPre(trace.getHead()).getTask() != this)
            return false;

        if (trigger("GOTO_" + this.getStateId())) {
            this.status = STATUS.ACCEPTED;
            trace.append(this, Trace.ORIGIN.WITHDRAW);
            log.info("task " + this.getName() + " uncompleted");
            return true;
        }

        log.info("task " + this.getName() + " fail to uncomplete");

        return false;

    }

    public boolean accept() {
        if (this.status != STATUS.PENDING)
            return false;

        this.status = STATUS.ACCEPTED;
        log.info("task " + this.getName() + " accepted");

        return true;
    }

    public boolean invoke(ActionExecutionContext actionExecutionContext) {

        if (this.status != STATUS.INITIALIZED)
            return false;

        log.info("task " + this.getName() + " invoked");

        this.status = STATUS.PENDING;

        this.trace.append(this);

        return true;

    }

    public boolean jump(Task task) throws ModelException {
        if (this.trace.getLatest(task) == null)
            return false;

        if (trigger("GOTO_" + task.getStateId())) {
            this.status = STATUS.COMPLETED;
            trace.append(task, Trace.ORIGIN.GOTO);
            log.info("jump to task " + task.getName() + " from " + this.getName());
            return true;
        }

        log.info("task " + this.getName() + " fail to jump");


        return false;
    }

    private Trace trace = null;

    private Flow flow = null;

    @Override
    public void execute(ActionExecutionContext actionExecutionContext) throws ModelException {

        if (!initialized) {
            initialization(actionExecutionContext);
        }

        this.taskId = UUID.randomUUID().toString();

        log.info("this :" + taskId);

        Object e = actionExecutionContext.getGlobalContext().get(SCXMLSystemContext.EVENT_KEY);
        if ((e instanceof TriggerEvent)) {
            this.predecessorId = ((TriggerEvent) e).getPayload().toString();
            log.info("this : " +taskId + ", predecessor :" + predecessorId);
        }


        EventVariable eventVariable = (EventVariable) actionExecutionContext.getGlobalContext().get(SCXMLSystemContext.EVENT_KEY);

        if (eventVariable != null)
            log.info("task " + this.getName() + " executing, triggered by event " + eventVariable.getName());

        this.invoke(actionExecutionContext);

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
