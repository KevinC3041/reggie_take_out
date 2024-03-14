package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Transactional
//    @Override
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        Long dishId = dishDto.getId(); //菜品id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());


        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新口味信息
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据--dish_flavor表的delete操作
        // delete from dish_flavor where dish_id = ???
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据--dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public void removeWithFlavor(Long id) {
        super.removeById(id);

        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        dishFlavorService.remove(queryWrapper);
    }

    @Override
    public void updateStatus0(String ids) {
//        //wrong code, cannot do in batch
//        Dish dish = this.getById(ids);
//        dish.setStatus(0);
//        super.updateById(dish);

        String[] idList = ids.split(",");
        if (idList.length == 1) {
            Long id = Long.parseLong(ids);
            Dish dish = new Dish();
            dish.setId(id);
            dish.setStatus(0);
            super.updateById(dish);
        } else if (idList.length > 1) {
            List<Dish> dishList = new ArrayList<>();
            for (String idS : idList) {
                Long id = Long.parseLong(idS);
                Dish temp = new Dish();
                temp.setStatus(0);
                temp.setId(id);
                dishList.add(temp);
            }
            super.updateBatchById(dishList);
        }


    }

    @Override
    public void updateStatus1(String ids) {
//        //wrong code, cannot do in batch
//        Dish dish = this.getById(ids);
//        dish.setStatus(1);
//        super.updateById(dish);

        List<String> idList = Arrays.asList(ids.split(","));
        if (idList.size() == 1) {
            Long id = Long.parseLong(ids);
            Dish dish = this.getById(id);
            dish.setStatus(1);
            this.updateById(dish);
        } else if (idList.size() > 1) {
            List<Dish> dishList = listByIds(idList);
            dishList.forEach((dish) -> {
                dish.setStatus(1);
            });
            this.updateBatchById(dishList);
        }
    }
}
