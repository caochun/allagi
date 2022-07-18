package info.nemoworks.allagi;


import com.google.common.io.Resources;
import info.nemoworks.allagi.model.Flow;
import info.nemoworks.allagi.model.Task;
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
        stateAssert(flow, "editing");
        Task next = flow.getTaskMap().get("editContent");
        createTask.uncomplete();
        stateAssert(flow, "init");
        createTask.jump(next);
        stateAssert(flow, "editing");
        TraceUtil.draw(flow.getTrace(), "trace.puml");
        System.out.println(flow.getTrace().getTrace().size());
    }

    @Test
    public void testFlowWithParallel() throws ModelException {
        Flow flow = new Flow(Resources.getResource("parallel.xml"));
        stateAssert(flow, "state1");
        Task task1 = flow.getTaskMap().get("task1");
        Task task2 = flow.getTaskMap().get("task2");
        Task task3 = flow.getTaskMap().get("task3");
        Task task4 = flow.getTaskMap().get("task4");
        task1.accept();
        task1.complete();
//        task2.accept();
//        task2.complete();
        task3.accept();
        task3.complete();
        task3.uncomplete();
        task3.jump(task2, task4);
        TraceUtil.draw(flow.getTrace(), "trace.puml");
    }

}
