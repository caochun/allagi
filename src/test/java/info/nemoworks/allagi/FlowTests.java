package info.nemoworks.allagi;


import com.google.common.io.Resources;
import info.nemoworks.allagi.model.Flow;
import info.nemoworks.allagi.model.Task;
import info.nemoworks.allagi.model.Trace;
import info.nemoworks.allagi.util.TraceUtil;
import org.apache.commons.scxml2.model.ModelException;
import org.junit.jupiter.api.Test;

import static info.nemoworks.allagi.util.FlowUtil.stateAssert;

public class FlowTests {

    @Test
    public void testFlowWithCustomAction() throws ModelException {
        Flow flow = new Flow(Resources.getResource("statetask.xml"));
        stateAssert(flow, "init");
        Task createTask = flow.getTaskMap().get("inputTitle");
        createTask.accept();
        createTask.complete();
        //createTask.trigger("create");
        stateAssert(flow, "editing");
        Task next = flow.getTaskMap().get("editContent");
        createTask.uncomplete();
        stateAssert(flow, "init");

        flow.getTrace().getHeadTask().jump(next);
        stateAssert(flow, "editing");
        TraceUtil.draw(flow.getTrace(), "trace.puml");
    }

    @Test
    public void testFlowWithParallel() throws ModelException {
        Flow flow = new Flow(Resources.getResource("parallel.xml"));
        stateAssert(flow, "state1");
        Task task1 = flow.getTaskMap().get("task1");
        task1.accept();
        task1.complete();
        TraceUtil.draw(flow.getTrace(), "trace.puml");
    }

}
