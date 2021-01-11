package org.prebid.pg.delstats.security;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

class CustomBasicAuthenticationEntryPointTest {
    SoftAssertions softAssertions;

    CustomBasicAuthenticationEntryPoint customBasicAuthenticationEntryPoint;

    @BeforeEach
    void setUp() {
        softAssertions = new SoftAssertions();

        customBasicAuthenticationEntryPoint = new CustomBasicAuthenticationEntryPoint();
    }

    @Test
    void commence() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        AuthenticationException authenticationException = new BadCredentialsException("test");
        softAssertions.assertThatCode(() ->customBasicAuthenticationEntryPoint
                .commence(mockHttpServletRequest, mockHttpServletResponse, authenticationException))
                .doesNotThrowAnyException();
        softAssertions.assertThat(mockHttpServletResponse.getHeaders("WWW-Authenticate"))
                .isNotEmpty();
        softAssertions.assertAll();
    }
}