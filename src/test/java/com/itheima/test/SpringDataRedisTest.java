package com.itheima.test;


import com.itheima.reggie.ReggieApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = ReggieApplication.class)
//@EnableAutoConfiguration
@RunWith(SpringRunner.class)
public class SpringDataRedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试StringRedisTemplate
     */
    @Test
    public void myTest() {
        stringRedisTemplate.opsForValue().set("test", "我的测试Func");
        String value = stringRedisTemplate.opsForValue().get("test");
        System.out.println(value);


    }

    /**
     * 操作String类型数据
     */
    @Test
    public void testString() {
        redisTemplate.opsForValue().set("city", "Shanghai宛平南路600号");

        String value = (String) redisTemplate.opsForValue().get("city");
        System.out.println(value);

        redisTemplate.opsForValue().set("key1", "value1", 10l, TimeUnit.SECONDS);

        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("city", "Beijing北京天安门");
        System.out.println(aBoolean);
    }

    /**
     * 操作哈希类型数据
     */
    @Test
    public void testHash() {

        HashOperations hashOperations = redisTemplate.opsForHash();

        //存值    HSET key field value
        hashOperations.put("002", "name", "xiaoming");
        hashOperations.put("002", "age", "20");
        hashOperations.put("002", "address", "bj");

        //取值    HGET key field
        String age = (String) hashOperations.get("002", "age");
        System.out.println(age);

        //获取hash结构中的所有字段    HKEYS key
        Set keys = hashOperations.keys("002");
        for (Object key : keys) {
            System.out.println(key);
        }

        //获取hash结构中的所有值    HVALS key
        List values = hashOperations.values("002");
        for (Object value : values) {
            System.out.println(value);
        }
    }

    /**
     * 操作List类型的数据
     */
    @Test
    public void testList() {
        ListOperations listOperations = redisTemplate.opsForList();

        //存值    LPUSH key value1 [value2]
        listOperations.leftPush("mylist", "a");
        listOperations.leftPushAll("mylist", "b", "c", "d");

        //取值    LRANGE key start stop
        List<String> mylist = listOperations.range("mylist", 0, -1);
        for (String value : mylist) {
            System.out.println(value);
        }

        //获得列表长度    LLEN key
        Long size = listOperations.size("mylist");
        int lSize = size.intValue();
        for (int i = 0; i < lSize; i++) {
            //出队列    RPOP key
            String element = (String) listOperations.rightPop("mylist");
            System.out.println(element);
        }
    }

    /**
     * 操作Set类型的数据
     */
    @Test
    public void testSet() {
        SetOperations setOperations = redisTemplate.opsForSet();

        //存值
        setOperations.add("myset", "a", "b", "c", "a");

        //取值
        Set<String> myset = setOperations.members("myset");
        for (String s : myset) {
            System.out.println(s);
        }

        //删除成员
        setOperations.remove("myset", "a", "b");

        myset = setOperations.members("myset");
        for (String s : myset) {
            System.out.println(s);
        }
    }

    /**
     * 操作ZSet类型的数据
     */
    @Test
    public void testZset() {
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();

        //存值
        zSetOperations.add("myZset", "a", 10.0);
        zSetOperations.add("myZset", "b", 11.0);
        zSetOperations.add("myZset", "c", 12.0);
        zSetOperations.add("myZset", "a", 13.0);

        //取值
        Set<String> myZset = zSetOperations.range("myZset", 0, -1);
        for (String s : myZset) {
            System.out.println(s);
        }

        //修改分数
        zSetOperations.incrementScore("myZset", "b", 20.0);

        //取值
        myZset = zSetOperations.range("myZset", 0, -1);
        for (String s : myZset) {
            System.out.println(s);
        }

        //删除成员
        zSetOperations.remove("myZset", "a", "b");

    }

    /**
     * 通用操作，针对不同的数据类型都可以操作
     */
    @Test
    public void testCommon() {
        // 获取Redis中所有的key
        Set<String> keys = redisTemplate.keys("*");
        for (String key : keys) {
            System.out.println(key);
        }

        // 判断某个key是否存在
        Boolean itcast = redisTemplate.hasKey("itcast");
        System.out.println(itcast);

        // 删除指定key
        redisTemplate.delete("myZset");

        // 获取指定key对应的value的数据类型
        DataType dataType = redisTemplate.type("myset");
        System.out.println(dataType.name());

    }


}
