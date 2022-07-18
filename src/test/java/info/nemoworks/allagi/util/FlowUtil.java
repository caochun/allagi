package info.nemoworks.allagi.util;

import info.nemoworks.allagi.model.Flow;
import org.apache.commons.scxml2.model.EnterableState;
import org.apache.commons.scxml2.model.TransitionTarget;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlowUtil {

    public static void stateAssert(Flow flow, String... stateIds) {
        Set<EnterableState> states = flow.getEngine().getCurrentStatus().getStates();
        Set<String> expectedIds = states.stream().map(EnterableState::getId).collect(Collectors.toSet());
        Set<String> assertIds = Arrays.stream(stateIds).collect(Collectors.toSet());
        assertEquals(assertIds, expectedIds);
        assertTrue(expectedIds.containsAll(assertIds));
    }
}
