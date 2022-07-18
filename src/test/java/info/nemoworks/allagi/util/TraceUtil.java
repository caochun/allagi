package info.nemoworks.allagi.util;

import info.nemoworks.allagi.model.Task;
import info.nemoworks.allagi.model.Trace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TraceUtil {

    public static void draw(Trace trace, String fileName) {
        File file = new File(fileName);
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
            writer.append("@startuml").append("\n");
            for (int i = 0; i < trace.getTrace().size(); i++) {
                Trace.Node node = trace.getTrace().get(i);
                writer.append("state ")
                        .append("node").append(String.valueOf(i + 1))
                        .append(" {\n");
                for (Trace.RecordNode recordNode : node.getRecordNodes()) {
                    Task task = recordNode.getTaskRecord();
                    writer.append("    ")
                            .append("state ")
                            .append("node").append(String.valueOf(i + 1))
                            .append(".")
                            .append(task.getName())
                            .append(".")
                            .append(task.getTaskId().replace("-", "").substring(0, 6))
                            .append(" {\n")
                            .append("    }\n");
                }
                writer.append("}\n");
            }

            for (int i = 0; i < trace.getTrace().size() - 1; i++) {
                writer.append("node").append(String.valueOf(i + 1))
                        .append(" -right-> ")
                        .append("node").append(String.valueOf(i + 2)).append("\n");
            }

            for (int i = 0; i < trace.getTrace().size(); i++) {
                Trace.Node node = trace.getTrace().get(i);
                writer.append("\"")
                        .append("node").append(String.valueOf(i + 1))
                        .append("\"")
                        .append(":")
                        .append(node.getOrigin().name())
                        .append("\n");

                for (Trace.RecordNode recordNode : node.getRecordNodes()) {
                    Task task = recordNode.getTaskRecord();
                    writer.append("\"")
                            .append("node").append(String.valueOf(i + 1))
                            .append(".")
                            .append(task.getName())
                            .append(".")
                            .append(task.getTaskId().replace("-", "").substring(0, 6))
                            .append("\"")
                            .append(":")
                            .append(task.getStatus().name())
                            .append("\n");
                }

            }

            writer.append("@enduml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public static void draw(Trace trace, String fileName) {
//        File file = new File(fileName);
//        if (!file.exists()) {
//            try {
//                boolean create = file.createNewFile();
//                if (!create) {
//                    throw new IOException("Failed to create file");
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        try (FileWriter writer = new FileWriter(file)) {
//            writer.append("@startuml");
//            writer.append("\n");
//            Task headTaskRecord = trace.getHead().getTaskRecord();
//            Set<EndpointPair<Trace.Node>> edges = trace.getEdges();
//            for (EndpointPair<Trace.Node> edge : edges) {
//                writer.append(edge.source().getTaskRecord().getName())
//                        .append(":")
//                        .append(edge.source().getTaskRecord().getTaskId().replace("-", "").substring(0, 6))
//                        .append(" --> ")
//                        .append(edge.target().getTaskRecord().getName()).append(":")
//                        .append(edge.target().getTaskRecord().getTaskId().replace("-", "").substring(0, 6))
//                        .append("\n");
//            }
//            writer.append(headTaskRecord.getName())
//                    .append(":")
//                    .append(headTaskRecord.getTaskId().replace("-", "").substring(0, 6))
//                    .append(" --> [*]")
//                    .append("\n");
//            Set<Trace.Node> nodes = trace.getNodes();
//            for (Trace.Node node : nodes) {
//                Task task = node.getTaskRecord();
//                writer.append("\"")
//                        .append(task.getName())
//                        .append(":")
//                        .append(task.getTaskId().replace("-", "").substring(0, 6))
//                        .append("\"")
//                        .append(":")
//                        .append(task.getStatus().name())
//                        .append("\n");
//                writer.append("\"")
//                        .append(task.getName())
//                        .append(":")
//                        .append(task.getTaskId().replace("-", "").substring(0, 6))
//                        .append("\"")
//                        .append(":")
//                        .append(node.getOrigin().name())
//                        .append("\n");
//            }
//
//            writer.append("@enduml");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
