package com.itheima.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;


/**
 * 通用的返回结果，服务端相应地数据最终都会封装成此对象
 * @param <T>
 */
@Data
public class R<T> {

    private Integer code; //编码：1成功，0和其它数字为失败
    private String msg; //错误信息
    private T data; //数据
    private Map map = new HashMap(); //动态数据

    public static <T> R<T> success(T object) {
        R<T> r = new R<T>();
        r.data = object;
        r.code = 1;
        return r;
    }

    public static <T> R<T> error(String msg) {
        R r = new R();
        r.msg = msg;
        r.code = 0;
        return r;
    }

    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}


//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Result {
//    private Integer code;//响应码，1 代表成功; 0 代表失败
//    private String msg;  //响应信息 描述字符串
//    private Object data; //返回的数据
//
//    //增删改 成功响应
//    public static Result success(){
//        return new Result(1,"success",null);
//    }
//    //查询 成功响应
//    public static Result success(Object data){
//        return new Result(1,"success",data);
//    }
//    //失败响应
//    public static Result error(String msg){
//        return new Result(0,msg,null);
//    }
//}
