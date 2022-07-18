package info.nemoworks.allagi.model;

import info.nemoworks.allagi.semantics.SCXMLGotoSemanticsImpl;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.SCXMLSemantics;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.SimpleErrorReporter;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.model.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Flow {

    private Log log;

    private SCXML stateMachine;

    @Getter
    private Map<String, Task> taskMap;

    @Getter
    private SCXMLExecutor engine;

    private Trace trace;

    public Flow(final URL scxmlDocument) throws ModelException {
        this(scxmlDocument, new JexlContext(), new JexlEvaluator());
    }

    public Flow(URL scxmlDocument, Context rootCtx, Evaluator evaluator) throws ModelException {
        log = LogFactory.getLog(this.getClass());

        trace = new Trace();

        rootCtx.set("trace", trace);
        rootCtx.set("flow", this);


        List<CustomAction> customActions = new ArrayList<CustomAction>();
        CustomAction ca = new CustomAction("https://nemoworks.info/", "task", Task.class);
        customActions.add(ca);

        try {
            stateMachine = SCXMLReader.read(scxmlDocument, new SCXMLReader.Configuration(null, null, customActions));
        } catch (IOException | XMLStreamException | ModelException ioe) {
            logError(ioe);
        }
        initialize(stateMachine, rootCtx, evaluator);
    }

    private void initialize(final SCXML stateMachine, final Context rootCtx, final Evaluator evaluator) throws ModelException {

        SCXMLSemantics semantics = new SCXMLGotoSemanticsImpl();
        engine = new SCXMLExecutor(evaluator, new SimpleDispatcher(), new SimpleErrorReporter(), semantics);
        engine.setStateMachine(stateMachine);
        engine.setRootContext(rootCtx);
        checkTask();
        try {
            engine.go();
        } catch (final ModelException me) {
            logError(me);
        }
    }

    private void checkTask() {
        // TODO: check if legal task definition
        taskMap = new HashMap<String, Task>();
        engine.getStateMachine().getTargets().values().stream()
                .filter(EnterableState.class::isInstance)
                .map(EnterableState.class::cast)
                .flatMap(enterableState -> Stream.concat(
                        enterableState.getOnEntries()
                                .stream().flatMap(onEntry -> onEntry.getActions().stream())
                                .filter(Task.class::isInstance),
                        enterableState.getOnExits()
                                .stream().flatMap(onExit -> onExit.getActions().stream())
                                .filter(Task.class::isInstance)
                ))
                .map(Task.class::cast)
                .forEach(task -> {
                    taskMap.put(task.getName(), task);
                });

    }

    public boolean resetMachine() {
        try {
            engine.reset();
        } catch (final ModelException me) {
            logError(me);
            return false;
        }
        return true;
    }

    protected void logError(final Exception exception) {
        if (log.isErrorEnabled()) {
            log.error(exception.getMessage(), exception);
        }
    }

    public Trace getTrace(){
        return this.trace;
    }

}
