package portaria.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthFilterCorsTest {
    @Test void browserPreflightPassesWithoutJwtButBusinessCallsRemainProtected() throws Exception {
        var jwt = mock(JwtUtil.class);
        var filter = new AuthFilter(jwt);
        var preflight = new MockHttpServletRequest("OPTIONS", "/blocos");
        preflight.setServletPath("/blocos");
        preflight.addHeader("Origin", "http://localhost:5173");
        preflight.addHeader("Access-Control-Request-Method", "GET");
        preflight.addHeader("Access-Control-Request-Headers", "authorization");
        var chain = new MockFilterChain();
        filter.doFilter(preflight, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
        verifyNoInteractions(jwt);

        var business = new MockHttpServletRequest("GET", "/blocos");
        business.setServletPath("/blocos");
        var response = new MockHttpServletResponse();
        var denied = new MockFilterChain();
        filter.doFilter(business, response, denied);
        assertEquals(401, response.getStatus());
        assertNull(denied.getRequest());
    }
}
