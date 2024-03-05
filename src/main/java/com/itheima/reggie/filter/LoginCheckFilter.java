package com.itheima.reggie.filter;


import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *检查用户是否已经完成登陆
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

//        1.获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("拦截到请求：{}",requestURI);

//        String requestURL = request.getRequestURL().toString();
//        log.info("URL: {}",requestURL);

//        定义不需要处理的请求路径

        String[] urls = new String[] {
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg", //移动端发送短信
                "/user/login", //移动端登陆
                "/user/loginout" //移动端登出
        };
        String[] urlsBackend = new String[] {
                "/employee/login",
                "/employee/logout",
                "/backend/**"
        };
        String[] urlsFront = new String[] {
                "/front/**",
                "/user/sendMsg", //移动端发送短信
                "/user/login", //移动端登陆
                "/user/loginout" //移动端登出
        };
        String[] urlsOther = new String[] {
                "/common/**",
        };


//        2.判断本次请求是否需要处理
          boolean check = check(urls, requestURI);
//        boolean check = check(urlsBackend, urlsFront, urlsOther, servletRequest);

//        3.如果不需要处理，则直接放行
        if (check) {
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

//        4-1.判断登陆状态，如果已登陆，则直接放行
        if(request.getSession().getAttribute("employee") != null) {
            log.info("员工用户已登陆，员工用户id为：{}", request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);

            return;
//            if(!requestURI.contains("/user/login")) return;
        }

//        log.info("员工用户未登录");
        if(!requestURI.contains("/user/login")) log.info("员工用户未登录");

//        4-2.判断登陆状态，如果已登陆，则直接放行
        if(request.getSession().getAttribute("user") != null && !(requestURI.contains("backend") || requestURI.contains("employee"))) {
            log.info("客人用户已登陆，客人用户id为：{}", request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentUserId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        log.info("客人用户未登录");
//        5.如果未登陆则返回未登录结果,通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urlsBackend
     * @param urlsFront
     * @param urlsOther
     * @param servletRequest
     * @return
     */
    public boolean check(String[] urlsBackend, String[] urlsFront, String[] urlsOther, ServletRequest servletRequest) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestURI = request.getRequestURI();

//        log.info("DEBUG here, if request.getSession().getAttribute() != null: {}", request.getSession().getAttribute("employee") != null);
        if (requestURI.contains("/index.html") || requestURI.contains("/login.html")) return true;
        for (String url : urlsBackend) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match && request.getSession().getAttribute("employee") != null) {
                return true;
            }
        }
        for (String url : urlsFront) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match && request.getSession().getAttribute("user") != null) {
                return true;
            }
        }
        for (String url : urlsOther) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }

        return false;
    }
    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }

}
