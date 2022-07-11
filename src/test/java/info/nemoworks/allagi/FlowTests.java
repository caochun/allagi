package info.nemoworks.allagi;


import com.google.common.graph.EndpointPair;
import com.google.common.io.Resources;
import info.nemoworks.allagi.model.Flow;
import info.nemoworks.allagi.model.Task;
import info.nemoworks.allagi.model.Trace;
import org.apache.commons.scxml2.model.ModelException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

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
        draw(flow);
    }

    public void stateAssert(Flow flow, String stateId) {
        assertEquals(
                flow.getEngine().getCurrentStatus().getStates().iterator().next().getId(),
                stateId);

    }

    public void draw(Flow flow) {
        File file = new File("flow.puml");
        if (!file.exists()) {
            try {
                boolean create = file.createNewFile();
                if (!create) {
                    throw new IOException("Failed to create file");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("@startuml");
            writer.append("\n");
            Task headTaskRecord = flow.getTrace().getHead().getTaskRecord();
            writer.append("[*] --> " + headTaskRecord.getName() + ":" + headTaskRecord.getTaskId().replace("-", "").substring(0, 6) + "\n");
            Set<EndpointPair<Trace.Node>> edges = flow.getTrace().getEdges();
            for (EndpointPair<Trace.Node> edge : edges) {
                writer.append(edge.source().getTaskRecord().getName())
                        .append(":")
                        .append(edge.source().getTaskRecord().getTaskId().replace("-", "").substring(0, 6))
                        .append(" --> ")
                        .append(edge.target().getTaskRecord().getName()).append(":")
                        .append(edge.target().getTaskRecord().getTaskId().replace("-", "").substring(0, 6))
                        .append("\n");
            }

            Set<Trace.Node> nodes = flow.getTrace().getNodes();
            for (Trace.Node node : nodes) {
                Task task = node.getTaskRecord();
                writer.append("\"")
                        .append(task.getName())
                        .append(":")
                        .append(task.getTaskId().replace("-", "").substring(0, 6))
                        .append("\"")
                        .append(":")
                        .append(task.getStatus().name())
                        .append("\n");
            }
            writer.append("@enduml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
