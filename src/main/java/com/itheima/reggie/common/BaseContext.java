package com.itheima.reggie.common;


/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登陆用户的id
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    private static ThreadLocal<Long> threadLocalUser = new ThreadLocal<>();

    /**
     * 设置值
     * @param id
     */
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    /**
     * 获取值
     * @return
     */
    public static Long getCurrentId(){
        return threadLocal.get();
    }

    public static void setCurrentUserId(Long id) { threadLocalUser.set(id); }

    public static Long getCurrentUserId(){
        return threadLocalUser.get();
    }

}
