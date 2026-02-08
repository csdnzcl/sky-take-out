package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        long total = page.getTotal();
        List<Setmeal> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    public void deleteByIds(List<Long> ids) {
        setmealMapper.deleteByIds(ids);
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        setmealMapper.update(setmeal);
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //查询套餐基本信息
        Setmeal setmeal = setmealMapper.getById(id);

        if(setmeal == null) {
            return null;
        }

        //查询套餐菜品信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        //封装到VO中
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        //单独设置关联菜品列表
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 获取套餐ID
        Long setmealId = setmealDTO.getId();

        //删除该套餐下的所有旧菜品
        // SQL: delete from setmeal_dish where setmeal_id = ?
        setmealDishMapper.deleteBySetmealId(setmealId);

        //重新插入新菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            // 注意：前端传来的菜品列表里，可能没有 setmealId，需要我们要手动赋值
            setmealDishes.forEach(dish -> {
                dish.setSetmealId(setmealId);
            });
            // 批量插入（复用新增套餐时的那个 Mapper 方法）
            setmealDishMapper.insertBatch(setmealDishes);

            setmealMapper.update(setmeal);
        }
    }

}
