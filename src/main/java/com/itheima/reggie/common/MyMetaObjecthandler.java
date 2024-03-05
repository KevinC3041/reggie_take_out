package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 自定义元数据对象处理器
 */

@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {

    @Autowired
    private HttpServletRequest servletRequest;

    public Integer myUriDetermineFunc() {

        String requestURI = servletRequest.getRequestURI();

        // 0:来自管理端backend和员工操作页面，1:来自用户端/移动端front和用户操作页面, -1:错误，我也不知道来自什么页面
        int returnValue;

        if (requestURI.contains("/employee") || requestURI.contains("/backend") || requestURI.contains("/category") || requestURI.contains("/dish") || requestURI.contains("/setmeal")) {
            returnValue = 0;
        } else if (requestURI.contains("/user") || requestURI.contains("/front") || requestURI.contains("/addressBook") || requestURI.contains("/shoppingCart") || requestURI.contains("/order/again")) {
            returnValue = 1;
        } else returnValue = -1;

        log.info("返回码为: {}, 拦截到请求：{}", returnValue, requestURI);

        return returnValue;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        log.info(metaObject.toString());

        //determine which page I am from
        int sign = myUriDetermineFunc();
        log.info("sign：{}。0管理，1用户，-1错误", sign);

//        log.info("Debug 1: {}", Arrays.toString(metaObject.getSetterNames()));
//        log.info("Debug 2: {}、{}", metaObject.hasSetter("createTime"), metaObject.hasSetter("updateTime"));

        String [] mySetters = {"createTime", "updateTime", "createUser", "updateUser", "isDeleted"};

        for (String mySetter : mySetters) {
            if (metaObject.hasSetter(mySetter) == true) {
                if (mySetter.contains("Time")) metaObject.setValue(mySetter, LocalDateTime.now());
                else if (sign == -1) throw new CustomException("Something went wrong, page from a unknown uri when inserting, uri: " + servletRequest.getRequestURI());
//                else if (mySetter.contains("User")) metaObject.setValue(mySetter, BaseContext.getCurrentId());
                else if (mySetter.contains("User") && sign == 0) metaObject.setValue(mySetter, BaseContext.getCurrentId());
                else if (mySetter.contains("User") && sign == 1) metaObject.setValue(mySetter, BaseContext.getCurrentUserId());
                else if (mySetter.equals("isDeleted")) metaObject.setValue("isDeleted", 0);
                else {
//                    log.info("Something went wrong, check it");
                    throw new CustomException("Something went wrong, check it");
                }
            }
        }

//        metaObject.setValue("createTime", LocalDateTime.now());
//        metaObject.setValue("updateTime", LocalDateTime.now());
//        metaObject.setValue("createUser", BaseContext.getCurrentId());
//        metaObject.setValue("updateUser", BaseContext.getCurrentId());
//        metaObject.setValue("isDeleted", 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        log.info(metaObject.toString());

        //determine which page I am from
        int sign = myUriDetermineFunc();
        log.info("sign：{}。0管理，1用户，-1错误", sign);

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}", id);

        metaObject.setValue("updateTime", LocalDateTime.now());
        if (sign == -1) throw new CustomException("Something went wrong, page from a unknown uri when updating, uri: " + servletRequest.getRequestURI());
        else if (sign == 0) metaObject.setValue("updateUser", BaseContext.getCurrentId());
        else if (sign == 1) metaObject.setValue("updateUser", BaseContext.getCurrentUserId());

//        metaObject.setValue("updateTime", LocalDateTime.now());
//        metaObject.setValue("updateUser", BaseContext.getCurrentId());
    }
}
