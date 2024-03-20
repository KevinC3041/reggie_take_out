package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}",setmealDto);

        setmealService.saveWithDish(setmealDto);

////        //清理所有套餐的缓存数据
////        Set keys = redisTemplate.keys("setmeal_*");
////        redisTemplate.delete(keys);
//
//        //清理某个分类下面的套餐缓存数据
//        String key = "setmeal_" + setmealDto.getCategoryId() + "_1";
//        redisTemplate.delete(key);

        return R.success("新增套餐成功");
    }


    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null, Setmeal::getName, name);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo,queryWrapper);

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();



        List<SetmealDto> list = records.stream().map((item)->{
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item, setmealDto);


            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);

        return R.success(dtoPage);
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {

        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);
    }

    /**
     * 更新套餐信息，同时更新套餐菜品的信息
     * @param setmealDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "setmealCache", key = "#setmealDto.categoryId + '_' + #setmealDto.status")
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info(setmealDto.toString());

        setmealService.updateWithSetmealDish(setmealDto);

//        //清理所有套餐的缓存数据
//        Set keys = redisTemplate.keys("setmeal_*");
//        redisTemplate.delete(keys);

//        //清理某个分类下面的套餐缓存数据
//        String key = "setmeal_" + setmealDto.getCategoryId() + "_1";
//        redisTemplate.delete(key);

        return R.success("套餐修改成功");
    }


    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids:{}", ids);

        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal) {
//        List<Setmeal> list = null;
//
//        //动态构造key
//        String key = "setmeal_" + setmeal.getCategoryId() + "_" + setmeal.getStatus();
//
//        //从Redis中获取缓存数据
//        list = (List<Setmeal>) redisTemplate.opsForValue().get(key);
//
//        if (list != null) {
//            //如果存在，直接返回，无需查询数据库
//            return R.success(list);
//        }

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

//        list = setmealService.list(queryWrapper);

        List<Setmeal> list = setmealService.list(queryWrapper);

//        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis
//        redisTemplate.opsForValue().set(key, list, 60, TimeUnit.MINUTES);

        return R.success(list);
    }

    /**
     * 停售套餐
     * @param ids
     * @return
     */
    @PostMapping("/status/0")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> discontinue(@RequestParam String ids) {
        log.info("需要修改状态的id号为: {}", ids);

        setmealService.discontinue(ids);

//        try {
//            setmealService.discontinue(ids);
//
//            //清理所有套餐的缓存数据
//            Set keys = redisTemplate.keys("setmeal_*");
//            redisTemplate.delete(keys);
//
//        } catch(Exception e) {
//            return R.error("Something went wrong here, please try again later");
//        }

        return R.success("套餐已停售");
    }

    /**
     * 启售套餐
     * @param ids
     * @return
     */
    @PostMapping("/status/1")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public R<String> startMenu(@RequestParam String ids) {
        log.info("需要修改状态的id号为: {}", ids);

        setmealService.startMenu(ids);

//        try {
//            setmealService.startMenu(ids);
//
//            //清理所有套餐的缓存数据
//            Set keys = redisTemplate.keys("setmeal_*");
//            redisTemplate.delete(keys);
//
//        } catch(Exception e) {
//            return R.error("Something went wrong here, please try again later");
//        }

        return R.success("套餐已启售");
    }



}
