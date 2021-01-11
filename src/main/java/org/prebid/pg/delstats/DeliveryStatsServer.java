package org.prebid.pg.delstats;

import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.AlertProxyConfiguration;
import org.prebid.pg.delstats.config.CorsConfiguration;
import org.prebid.pg.delstats.config.ServerAuthDataConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.controller.filter.PeekFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableSwagger2
public class DeliveryStatsServer {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatsServer.class);

    @Autowired
    private ServerAuthDataConfiguration serverAuthDataConfiguration;

    @Autowired
    private CorsConfiguration corsConfiguration;

    @Autowired
    private ServerConfiguration serverConfiguration;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        logger.info("Starting application ...");
        ApplicationContext ctx = SpringApplication.run(DeliveryStatsServer.class, args);
        logger.debug("Application ready");
        ctx.getBean(AlertProxyHttpClient.class).sendNotification(AlertName.START_UP,
                "Service started", AlertPriority.NOTIFICATION);
        logger.info("Application ready, Start up notice prepared");
    }

    @Bean
    public JettyServletWebServerFactory jettyServletWebServerFactory() {
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        logger.info("JettyGzipHandlerEnabled={}", serverConfiguration.isJettyGzipHandlerEnabled());
        if (serverConfiguration.isJettyGzipHandlerEnabled()) {
            factory.addServerCustomizers(server -> {
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setInflateBufferSize(1);
                gzipHandler.setHandler(server.getHandler());
                HandlerCollection handlerCollection = new HandlerCollection(gzipHandler);
                server.setHandler(handlerCollection);
            });
        }
        return factory;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                configureCorsRegistry(registry);

            }
        };
    }

    void configureCorsRegistry(CorsRegistry registry) {
        if (!corsConfiguration.isEnabled() || !Boolean.TRUE.equals(serverAuthDataConfiguration.getEnabled())) {
            logger.warn("CORS handling disabled");
            return;
        }

        if (logger.isWarnEnabled()) {
            logger.info("CORS::pathPattern={}", corsConfiguration.getPathPattern());
            logger.info("CORS::isAllowCredentials={}", corsConfiguration.isAllowCredentials());
            logger.info("CORS::allowHeaders={}", String.join(",", corsConfiguration.getAllowHeaders()));
            logger.info("CORS::allowOrigins={}", String.join(",", corsConfiguration.getAllowOrigins()));
            logger.info("CORS::allowMethods={}", String.join(",", corsConfiguration.getAllowMethods()));
            logger.info("CORS::maxAge={}", corsConfiguration.getMaxAgeSec());
        }

        registry.addMapping(corsConfiguration.getPathPattern())
                .allowCredentials(corsConfiguration.isAllowCredentials())
                .allowedHeaders(corsConfiguration.getAllowHeaders())
                .allowedOrigins(corsConfiguration.getAllowOrigins())
                .allowedMethods(corsConfiguration.getAllowMethods())
                .maxAge(corsConfiguration.getMaxAgeSec());
    }

    @Bean
    public RestTemplate getRestTemplate(AlertProxyConfiguration alertProxyConfiguration) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(alertProxyConfiguration.getTimeoutSec() * 1000);
        factory.setReadTimeout(alertProxyConfiguration.getTimeoutSec() * 1000);

        return new RestTemplate(factory);
    }

    @Configuration
    @ConditionalOnProperty(value = "api.peek.enabled", havingValue = "true")
    public class FilterConfiguration {
        @Bean
        public FilterRegistrationBean<PeekFilter> filterRegistrationBean() {
            FilterRegistrationBean<PeekFilter> registrationBean = new FilterRegistrationBean<>();
            registrationBean.setFilter(new PeekFilter());
            registrationBean.addUrlPatterns("/*");
            return registrationBean;
        }
    }
}
