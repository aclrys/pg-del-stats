package org.prebid.pg.delstats.config;

import org.prebid.pg.delstats.security.CustomBasicAuthenticationEntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    private static final String REGEX = "\\s*,[,\\s]*";

    public static final String ADMIN_ROLE = "admin";

    @Autowired
    private ServerAuthDataConfiguration serverAuthDataConfiguration;

    @Autowired
    private CustomBasicAuthenticationEntryPoint authenticationEntryPoint;

    @Value("${services.base-url}")
    private String baseUrl;

    @Value("${server-api-roles.get-delivery-report}")
    private String getDeliveryReportRoles;

    @Value("${server-api-roles.post-delivery-report}")
    private String postDeliveryReportRoles;

    @Value("${server-api-roles.get-token-spend-report}")
    private String getTokenSpendReportRoles;

    @Value("${server-api-roles.get-line-item-summary}")
    private String getLineItemSummaryRoles;

    @Value("${server-api-roles.get-delivery-summary-freshness}")
    private String getDeliverySummaryFreshnessRoles;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        logger.info("serverAuthDataConfiguration={}", serverAuthDataConfiguration);
        for (final ServerAuthDataConfiguration.Principal p : serverAuthDataConfiguration.getPrincipals()) {
            auth.inMemoryAuthentication()
                    .withUser(p.username).password(passwordEncoder().encode(p.password))
                    .roles(p.roles.split(REGEX));
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Only disabled if explicitly set to false (hence use of Boolean; default value is null)
        if (Boolean.FALSE.equals(serverAuthDataConfiguration.enabled)) {
            logger.warn("Application Running with no Authentication and CSRF disabled!");
            http.csrf().disable();
            return;
        }

        http
                .antMatcher(baseUrl + "/**")
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, baseUrl + "/v1/report/token-spend")
                    .hasAnyRole(getRoles(getTokenSpendReportRoles))
                .antMatchers(HttpMethod.GET, baseUrl + "/v1/report/delivery")
                    .hasAnyRole(getRoles(getDeliveryReportRoles))
                .antMatchers(HttpMethod.GET, baseUrl + "/v2/report/delivery")
                    .hasAnyRole(getRoles(getDeliveryReportRoles))
                .antMatchers(HttpMethod.POST, baseUrl + "/v1/report/delivery")
                    .hasAnyRole(getRoles(postDeliveryReportRoles))
                .antMatchers(HttpMethod.GET, baseUrl + "/v1/report/line-item-summary")
                    .hasAnyRole(getRoles(getLineItemSummaryRoles))
                .antMatchers(HttpMethod.POST, baseUrl + "/v1/prep-for-shutdown")
                    .hasRole(ADMIN_ROLE)
                .antMatchers(HttpMethod.POST, baseUrl + "/v1/cease-shutdown")
                    .hasRole(ADMIN_ROLE)
                .antMatchers(HttpMethod.POST, baseUrl + "/v1/tracer")
                    .hasRole(ADMIN_ROLE)
                .antMatchers(HttpMethod.GET, baseUrl + "/v1/report/summary/freshness")
                    .hasAnyRole(getRoles(getDeliverySummaryFreshnessRoles))
                .antMatchers(HttpMethod.GET, baseUrl + "/v1/create/line-item-summary")
                    .hasRole(ADMIN_ROLE)
            .and()
                .httpBasic()
            .and()
                .csrf().disable();
    }

    private String[] getRoles(String resourceRole) {
        String resourceRoleWithAdmin = resourceRole.concat(",").concat(ADMIN_ROLE);
        return resourceRoleWithAdmin.split(REGEX);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return getPasswordEncoder();
    }

    private PasswordEncoder getPasswordEncoder() {
        return NoOpEncoder.getInstance();
    }
}

class NoOpEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.toString().equals(encodedPassword);
    }

    public static PasswordEncoder getInstance() {
        return INSTANCE;
    }

    private static final PasswordEncoder INSTANCE = new NoOpEncoder();

    private NoOpEncoder() {
    }
}
