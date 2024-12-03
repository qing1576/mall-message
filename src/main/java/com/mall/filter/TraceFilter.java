package com.mall.filter;

import com.mall.config.PublicConfig;
import com.mall.util.GsonUtil;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <b><u>TraceFilter功能说明：</u></b>
 * <p>添加调用链路id</p>
 * @author
 * 2023-03-08 11:16
 */
@Component
public class TraceFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static String UNKNOWN = "unknown";

    public static final String UTF_8 = "UTF-8";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String url = request.getRequestURI();
        // 此处需要放过druid的请求，否则请求参数会被customHttpServletRequestWrapper破坏组件原有的结构
        return url.startsWith("/druid");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        request.setCharacterEncoding(UTF_8);
        // 获取接口的方法类型
        String method = request.getMethod();
        // 获取接口的地址。相对路径，不包含参数
        String url = request.getRequestURI();
        // 接口参数。适用于GET方法拼接的方式
        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            queryString = java.net.URLDecoder.decode(queryString, UTF_8);
            url = url + "?" + queryString;
        }

        // 添加traceId
        String traceId = request.getHeader(PublicConfig.HEADER_TRACEID);

        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
            logger.info("生成新的traceId: {}", traceId);
        }
        // 写入MDC，供日志使用
        MDC.put(PublicConfig.HEADER_TRACEID, traceId);
        MDC.put(PublicConfig.THREAD_NAME, Thread.currentThread().getName());
        logger.info("traceId = {}", traceId);

        // 获取ip
        String ip = getIpAddr(request);
        if (!StringUtils.hasText(ip)) {
            ip = "undefined";
        }

        String body;
        ServletRequest servletRequest = request;
        // Content-Type为multipart/form-data类型的参数值，需要从getParameter()中获取。其它类型的，可以从getReader()获取
        if (Objects.nonNull(request.getContentType())
                && request.getContentType().contains(ContentType.MULTIPART_FORM_DATA.getMimeType())) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            body = GsonUtil.toJson(parameterMap);
            if (!CollectionUtils.isEmpty(request.getParts())) {
                Part part;
                StringBuffer fileName = new StringBuffer();
                int size = request.getParts().size();
                Part[] parts = request.getParts().toArray(new Part[size]);
                for (int i = 0; i < size; i++) {
                    part = parts[i];
                    String fileNameStr = part.getSubmittedFileName() + "(" + part.getSize() + "Byte)";
                    if (i == 0) {
                        fileName.append(fileNameStr);
                    } else {
                        fileName.append(", ").append(fileNameStr);
                    }
                }
                body = body + ", fileName = " + fileName;
            }
        } else {
            servletRequest = new CustomHttpServletRequestWrapper(request);
            body = ((CustomHttpServletRequestWrapper) servletRequest).getBody();
        }

        // 获取POST方式的请求参数body
        if (StringUtils.hasText(body)) {
            logger.info("{}, method = {}, ip = {}, body = {}", url, method, ip, body);
        } else {
            logger.info("{}, method = {}, ip = {}", url, method, ip);
        }

        filterChain.doFilter(servletRequest, response);
    }

    /**
     * <b><u>getIpAddr方法说明：</u></b>
     * <p>获取用户真实IP地址，不使用request.getRemoteAddr()的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值</p>
     * @param request   请求
     * @return java.lang.String 请求ip
     * @author
     * 2023-03-08 12:53
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !UNKNOWN.equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
