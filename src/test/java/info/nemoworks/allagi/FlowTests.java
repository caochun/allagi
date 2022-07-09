package info.nemoworks.allagi;


import com.google.common.io.Resources;
import info.nemoworks.allagi.model.Flow;
import info.nemoworks.allagi.model.Task;
import org.apache.commons.scxml2.model.ModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowTests {

    @Test
    public void testFlowWithCustomAction() throws ModelException {
        Flow flow = new Flow(Resources.getResource("statetask.xml"));
        stateAssert(flow, "init");
        Task createTask = flow.getTrace().getHeadTask();
        createTask.accept();
        createTask.complete();
        //createTask.trigger("create");
        stateAssert(flow, "editing");
        Task next = flow.getTrace().getHeadTask();
        createTask.uncomplete();
        stateAssert(flow, "init");

        flow.getTrace().getHeadTask().jump(next);
        stateAssert(flow, "editing");
    }

    public void stateAssert(Flow flow, String stateId) {
        assertEquals(
                flow.getEngine().getCurrentStatus().getStates().iterator().next().getId(),
                stateId);

    }

}
