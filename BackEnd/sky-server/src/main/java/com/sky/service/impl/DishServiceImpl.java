package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     *
     * 这个方法需要操作两张表，所以要加上@Transactional注解保证事务原子性
     * 菜品和口味数据要么全部插入成功，要么全部失败回滚
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 向菜品表插入1条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> { // 设置菜品id
                dishFlavor.setDishId(dishId);
            });
            //插入数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     * TODO 后期可以尝试在删除菜品时把阿里云服务器中的菜品图片也删除掉
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1、判断当前菜品是否能够删除——是否存在启售中的菜品？
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                // 如果当前菜品处于启售中，则不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 2、判断当前菜品是否能够删除——是否被某个套餐关联了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0) {
            // 如果当前菜品被某个套餐关联了，则不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 3、根据菜品id集合批量删除菜品数据
        // SQL: delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);

        // 4、根据菜品id集合批量删除关联的口味数据
        // SQL: delete from dish_flavor where dish_id in (?,?,?)
        dishFlavorMapper.deleteByDishIds(ids);

        // 已废弃的删除菜品数据代码
        //for (Long id : ids) {
        //    dishMapper.deleteById(id);
        //    dishFlavorMapper.deleteByDishId(ids);
        //}
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 将查询结果封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改菜品表基本信息
        dishMapper.update(dish);

        // 删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> { // 设置菜品id
                dishFlavor.setDishId(dishDTO.getId());
            });
            //插入数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品启售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        // 如果是停售操作的话，还需要将包含当前菜品的套餐也停售
        if(status.equals(StatusConstant.ENABLE)) {
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // SQL: select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if(setmealIds != null && setmealIds.size() > 0) {
                for(Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    /**
     * 根据分类id查询启用状态的菜品列表
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> list(Long categoryId) {
        // 查询启用状态的菜品
        List<Dish> dishList = dishMapper.list(categoryId, StatusConstant.ENABLE);
        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish dish : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            // 查询口味信息
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }
}
