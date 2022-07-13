package info.nemoworks.allagi.util;

import info.nemoworks.allagi.model.Flow;
import org.apache.commons.scxml2.model.EnterableState;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowUtil {

    public static void stateAssert(Flow flow, String stateId) {
        Set<EnterableState> states = flow.getEngine().getCurrentStatus().getStates();
        assertEquals(states.size(), 1);
        assertEquals(states.iterator().next().getId() , stateId);

    }
}
