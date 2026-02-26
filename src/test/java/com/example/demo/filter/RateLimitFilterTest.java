package com.example.demo.filter;

import com.example.demo.config.RateLimitProperties;
import com.example.demo.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitFilter rateLimitFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        rateLimitFilter = new RateLimitFilter(rateLimitService, properties);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRemoteAddr("192.168.1.1");
    }

    @Test
    void shouldPassThroughWhenWithinLimit() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        when(rateLimitService.isAllowed("192.168.1.1")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        when(rateLimitService.isAllowed("192.168.1.1")).thenReturn(false);
        when(rateLimitService.getRetryAfterSeconds("192.168.1.1")).thenReturn(45L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(429, response.getStatus());
    }

    @Test
    void shouldIncludeRetryAfterHeader() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        when(rateLimitService.isAllowed("192.168.1.1")).thenReturn(false);
        when(rateLimitService.getRetryAfterSeconds("192.168.1.1")).thenReturn(45L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals("45", response.getHeader("Retry-After"));
    }

    @Test
    void shouldReturnJsonResponseBody() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        when(rateLimitService.isAllowed("192.168.1.1")).thenReturn(false);
        when(rateLimitService.getRetryAfterSeconds("192.168.1.1")).thenReturn(45L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals("application/json", response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains("\"error\":\"Too Many Requests\""));
        assertTrue(content.contains("\"retryAfter\":45"));
    }

    @Test
    void shouldExtractIpFromXForwardedForHeader() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        when(rateLimitService.isAllowed("10.0.0.1")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).isAllowed("10.0.0.1");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallbackToRemoteAddrWhenNoXForwardedFor() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        request.setRemoteAddr("192.168.1.100");
        when(rateLimitService.isAllowed("192.168.1.100")).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).isAllowed("192.168.1.100");
    }

    @Test
    void shouldNotFilterActuatorEndpoints() {
        request.setRequestURI("/actuator/health");
        assertTrue(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterHealthEndpoint() {
        request.setRequestURI("/health");
        assertTrue(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldFilterApiEndpoints() {
        request.setRequestURI("/api/users");
        assertFalse(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterWhenDisabled() {
        properties.setEnabled(false);
        request.setRequestURI("/api/test");
        assertTrue(rateLimitFilter.shouldNotFilter(request));
    }
}
