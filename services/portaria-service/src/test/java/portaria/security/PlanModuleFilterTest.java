package portaria.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PlanModuleFilterTest {
    @Test void checksDirectRequestsAndFailsClosed() throws Exception {
        AtomicInteger upstream = new AtomicInteger(403);
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/plans/access/entregas", exchange -> {
            calls.incrementAndGet();
            assertEquals("Bearer test-token", exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(upstream.get(), -1);
            exchange.close();
        });
        server.start();
        PlanModuleFilter filter = new PlanModuleFilter("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
                MockHttpServletRequest req = new MockHttpServletRequest(method, "/entregas/1");
                req.setServletPath("/entregas/1");
                req.addHeader("Authorization", "Bearer test-token");
                MockHttpServletResponse res = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();
                filter.doFilter(req, res, chain);
                assertEquals(403, res.getStatus());
                assertNull(chain.getRequest());
            }
            upstream.set(204);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/entregas");
            req.setServletPath("/entregas");
            req.addHeader("Authorization", "Bearer test-token");
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(req, new MockHttpServletResponse(), chain);
            assertNotNull(chain.getRequest());
            upstream.set(500);
            MockHttpServletResponse unavailable = new MockHttpServletResponse();
            filter.doFilter(req, unavailable, new MockFilterChain());
            assertEquals(503, unavailable.getStatus());
            MockHttpServletRequest anonymous = new MockHttpServletRequest("GET", "/entregas");
            anonymous.setServletPath("/entregas");
            MockHttpServletResponse denied = new MockHttpServletResponse();
            filter.doFilter(anonymous, denied, new MockFilterChain());
            assertEquals(401, denied.getStatus());
            assertEquals(7, calls.get());
        } finally {
            server.stop(0);
        }
    }
}
