package org.prebid.pg.delstats.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint to activate and deactivate tracing logging throughout the application.
 */
@Slf4j
@RestController
@RequestMapping("${services.admin-base-url}")
@Api(tags = "admin")
public class TracerController {

    private Tracer tracer;

    public TracerController(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Set tracer settings. The settings either disable tracing or enable it or enable tracing on specific field values.
     *
     * @param tracerIn
     */
    @PostMapping(value = "/v1/tracer")
    @ResponseStatus(HttpStatus.OK)
    public void setTrace(@RequestBody Tracer tracerIn) {

        log.info("old tracer=" + tracer);
        tracer.setTracer(tracerIn);
        log.info("new tracer=" + tracer);
    }

    /**
     * Retrieve the current tracer settings.
     * @return
     */
    @GetMapping(value = "/v1/tracer")
    public Tracer getTrace() {
        return tracer;
    }
}
