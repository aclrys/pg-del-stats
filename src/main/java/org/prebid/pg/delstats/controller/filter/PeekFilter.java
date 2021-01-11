package org.prebid.pg.delstats.controller.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

public class PeekFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(PeekFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("PeekFilter::init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = null;
        if (request instanceof HttpServletRequest) {
            httpRequest = (HttpServletRequest) request;
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if (name.toUpperCase().contains("ENCODING")) {
                        logger.info("{}:{}", name, httpRequest.getHeader(name));
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}

