package org.seoulhub.backend.global.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
public class RequestLimitFilter extends OncePerRequestFilter {
    private final RequestRateLimiter limiter;
    private final String proxySecret;
    public RequestLimitFilter(RequestRateLimiter limiter, String proxySecret) { this.limiter=limiter; this.proxySecret=proxySecret; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getRequestURI();
        if(path.startsWith("/api/") && !req.getMethod().equals("OPTIONS")) {
            if(!proxySecret.isBlank() && !java.security.MessageDigest.isEqual(proxySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.util.Objects.toString(req.getHeader("X-Internal-API-Key"), "").getBytes(java.nio.charset.StandardCharsets.UTF_8))) { reply(res,403,"허용되지 않은 API 접근입니다.");return; }
            if(req.getContentLengthLong()>32768) { reply(res,413,"요청 내용이 너무 큽니다.");return; }
            boolean auth=path.startsWith("/api/v1/users/");
            int limit=auth ? 120 : 600;
            // Never trust browser-supplied X-Forwarded-For. This is a process-wide/IP backstop.
            if(!limiter.allow("ip:"+req.getRemoteAddr()+":"+auth,limit,60000)) { res.setHeader("Retry-After","60");reply(res,429,"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");return; }
        }
        chain.doFilter(req,res);
    }
    private void reply(HttpServletResponse res,int status,String message) throws IOException {
        res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\""+message+"\"}");
    }
}
