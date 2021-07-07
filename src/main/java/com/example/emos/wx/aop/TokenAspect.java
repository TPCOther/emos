package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect     //切面类，在切点拦截下响应并插入刷新的token
@Component
public class TokenAspect {
    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..)))") //设定切点
    public void aspect(){}

    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable{
        R r = (R)point.proceed();
        String token = threadLocalToken.getToken();
        if(token!=null){ //若token更新，将token加入返回响应中
            r.put("token",token);
            threadLocalToken.clear();
        }
        return r;
    }
}
