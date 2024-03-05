package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RequestMapping("/order")
@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据：{}", orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }


    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {

        log.info("page:{}, pageSize:{}, number:{}, beginTime:{}, endTime:{}", page, pageSize, number, beginTime, endTime);

        //构造分页构造器对象
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //添加根据订单号查询
        queryWrapper.like(StringUtils.isNotEmpty(number), Orders::getNumber, number);
        //添加根据时间范围查询
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        LocalDateTime bTime = LocalDateTime.parse(beginTime, formatter);
//        LocalDateTime eTime = LocalDateTime.parse(endTime, formatter);
        queryWrapper.between((StringUtils.isNotEmpty(beginTime) && StringUtils.isNotEmpty(endTime)), Orders::getOrderTime, beginTime, endTime);


        //执行分页查询
        orderService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }


    //前端一定有bug，它调了两次api，我找不出来错误
    /**
     * 前端订单信息分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize) {

        //构造分页构造器对象
        Page<Orders> pageInfo = new Page<>(page, pageSize);

//        Long userId = BaseContext.getCurrentId();
        Long userId = BaseContext.getCurrentUserId();

        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //筛选用户条件
        queryWrapper.eq(Orders::getUserId, userId);
        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        //执行分页查询
        orderService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 商家派送，修改订单状态
     * @param order
     * @return
     */
    @PutMapping
    public R<String> deliver(@RequestBody Orders order) {
        log.info("Order:{}", order);
        orderService.updateById(order);
        return R.success("派送成功");
    }

    /**
     * 用户再来一单
     * @param map
     * @return
     */
    @PostMapping("/again")
    public R<String> orderAgain(@RequestBody Map map) {
        log.info("map: {}", map);
        try {
            orderService.orderAgain(map);
        } catch (Exception e) {
            log.info("Error message: {}", e.getMessage());
            return R.error("Something went wrong, please try again.");
        }
        return R.success("下单成功");
    }

}
