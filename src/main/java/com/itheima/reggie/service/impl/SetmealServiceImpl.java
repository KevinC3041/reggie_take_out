package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
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
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item)-> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，操作setmeal_dish，执行insert操作
        setmealDishService.saveBatch(setmealDishes);


    }


    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        //select count(*) from setmeal where id in (id1,id2,id3) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);

        int count = this.count(queryWrapper);
        if (count > 0) {
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除套餐表中的数据--setmeal
        this.removeByIds(ids);

        //删除关系表中的数据--setmeal_dish
        // delete from setmeal_dish where setmeal_id in (id1, id2, id3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }

    /**
     * 停售套餐
     * @param ids
     */
    @Override
    public void discontinue(String ids) {
        String[] idList = ids.split(",");
        if (idList.length == 1) {
            Long id = Long.parseLong(ids);
            Setmeal setmeal = new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(0);
            super.updateById(setmeal);
        } else if (idList.length > 1) {
            List<Setmeal> setmealList = new ArrayList<>();
            for (String idS : idList) {
                Long id = Long.parseLong(idS);
                Setmeal temp = new Setmeal();
                temp.setStatus(0);
                temp.setId(id);
                setmealList.add(temp);
            }
            super.updateBatchById(setmealList);
        }
    }

    /**
     * 启售套餐
     * @param ids
     */
    @Override
    public void startMenu(String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        if (idList.size() == 1) {
            Long id = Long.parseLong(ids);
            Setmeal setmeal = this.getById(id);
            setmeal.setStatus(1);
            this.updateById(setmeal);
        } else if (idList.size() > 1) {
            List<Setmeal> setmealList = listByIds(idList);
            setmealList.forEach((setmeal) -> {
                setmeal.setStatus(1);
            });
            this.updateBatchById(setmealList);
        }

    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {

        //查询套餐基本信息，从setmeal表中查询
        Setmeal setmeal = this.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        //查询当前套餐对应的分类名称，从category表中查询
        LambdaQueryWrapper<Category> queryWrapperC = new LambdaQueryWrapper<>();
        queryWrapperC.eq(Category::getId, setmealDto.getCategoryId());
        Category category = categoryService.getOne(queryWrapperC);
        setmealDto.setCategoryName(category.getName());

        //查询当前套餐对应的菜品信息，从setmeal_dish表中查询
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        List<SetmealDish> dishes = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(dishes);

        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新套餐菜品的信息
     * @param setmealDto
     */
    @Override
    @Transactional
    public void updateWithSetmealDish(SetmealDto setmealDto) {

        //更新setmeal表基本信息
        this.updateById(setmealDto);

        //清理当前套餐对应的菜品数据--setmeal_dish表的delete操作
        // delete from setmeal_dish where setmeal_id = ???
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());

        setmealDishService.remove(queryWrapper);

        //添加当前提交过来的菜品数据--setmeal_dish表的insert操作
        List<SetmealDish> dishes = setmealDto.getSetmealDishes();

        dishes = dishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(dishes);
    }
}
