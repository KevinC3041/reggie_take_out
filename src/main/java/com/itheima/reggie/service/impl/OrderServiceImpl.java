package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     */
    @Transactional
    public void submit(Orders orders) {
        //获得当前用户id
//        Long userId = BaseContext.getCurrentId();
        Long userId = BaseContext.getCurrentUserId();

        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        //查询用户数据
        User user = userService.getById(userId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }

        long orderId = IdWorker.getId();//订单号

        AtomicInteger amount = new AtomicInteger(0);

        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        //向订单表插入数据，一条数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get())); //总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //清空购物车数据
        shoppingCartService.remove(wrapper);
    }

    /**
     * 用户再来一单
     * @param map
     */
    @Override
    public void orderAgain(Map map) {
        //String -> Long
        Long orderId = Long.parseLong((String)map.get("id"));
        //查询当前用户id
        Long userId = BaseContext.getCurrentUserId();

        //查询当前订单号的订单明细表数据
        //select * from order_detail where order_id = ?
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetails = orderDetailService.list(wrapper);

        if (orderDetails == null || orderDetails.size() == 0) {
            throw new CustomException("你从哪选到的再来一单？");
        }

        //查询当前购物车数据
//        select * from shopping_cart where user_id = ?
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
////        select name, number from shopping_cart where user_id = ?
//        queryWrapper.eq(ShoppingCart::getUserId, userId).select(ShoppingCart::getName, ShoppingCart::getNumber);
        List<ShoppingCart> oldShoppingCart = shoppingCartService.list(queryWrapper);

        List<ShoppingCart> shoppingCartList = orderDetails.stream().map((item) -> {

            String name = item.getName();
            ShoppingCart shoppingCart = new ShoppingCart();

            Optional<ShoppingCart> matchingCart = oldShoppingCart.stream().filter(cartItem -> name.equals(cartItem.getName())).findFirst();
            if (matchingCart.isPresent()) { // cargo already in the cart, just change and update the amount
                ShoppingCart matchedOne = matchingCart.get();
                BeanUtils.copyProperties(matchedOne, shoppingCart);
                shoppingCart.setNumber(shoppingCart.getNumber() + item.getNumber());
            } else { // cargo not in the cart, update all
                if(item.getSetmealId() != null) shoppingCart.setSetmealId(item.getSetmealId());
                shoppingCart.setName(name);
                shoppingCart.setImage(item.getImage());
                shoppingCart.setUserId(userId);
                if(item.getDishId() != null) shoppingCart.setDishId(item.getDishId());
                if(item.getDishFlavor() != null) shoppingCart.setDishFlavor(item.getDishFlavor());
                shoppingCart.setNumber(item.getNumber());
                shoppingCart.setAmount(item.getAmount());
            }
            return shoppingCart;
        }).collect(Collectors.toList());

        //向购物车表插入多条数据
        shoppingCartService.saveOrUpdateBatch(shoppingCartList);
    }
}
