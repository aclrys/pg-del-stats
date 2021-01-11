package org.prebid.pg.delstats.model.dto;

import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.controller.TracerController;
import org.prebid.pg.delstats.model.dto.Filters;
import org.prebid.pg.delstats.model.dto.Tracer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TracerControllerTest {

    @Test
    void shouldGetSetTracer() {
        Filters filter = Filters.builder()
                .accountId("testAI")
                .bidderCode("testBC")
                .lineItemId("testLI")
                .region("testR")
                .vendor("testV")
                .build();
        Tracer oldTracer = Tracer.builder()
                .enabled(true)
                .durationInSeconds(5)
                .filters(filter)
                .expiresAt(Instant.MAX)
                .raw(true)
                .build();
        Tracer newTracer = Tracer.builder()
                .enabled(false)
                .durationInSeconds(5)
                .filters(filter)
                .expiresAt(Instant.MAX)
                .raw(false)
                .build();
        TracerController tracerController = new TracerController(oldTracer);
        assertTrue(tracerController.getTrace() == oldTracer);
        tracerController.setTrace(newTracer);
        assertTrue(!tracerController.getTrace().isActive());
    }
}