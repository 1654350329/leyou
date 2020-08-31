package com.leyou.item.service.impl;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tk.mybatis.mapper.entity.Example;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CategortServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父节点id查询分类
     * @param pid
     * @return
     */
    @Override
    public List<Category> queryCategoriesByPid(Long pid) {
        Example example = new Example(Category.class);
        example.createCriteria().andEqualTo("parentId",pid);
        List<Category> list = this.categoryMapper.selectByExample(example);

        return list;
    }


    /**
     * 根据品牌id查询分类
     * @param bid
     * @return
     */
    @Override
    public List<Category> queryCategoriesByBrandId(Long bid) {
       List<Category> categories = this.categoryMapper.selectCategoriesByBrandId(bid);
        return categories;
    }

    @Override
    public List<String> queryNamesById(List<Long> ids) {
        List<String> names=new ArrayList<>();
        ids.forEach(id->{
            Category category = this.categoryMapper.selectByPrimaryKey(id);
            names.add(category.getName());
        });

        return names;
    }

    @Override
    public List<Category> queryCategoryByCid(Long cid) {
        Category category3 = this.categoryMapper.selectByPrimaryKey(cid);
        Category category2 = this.categoryMapper.selectByPrimaryKey(category3.getParentId());
        Category category1 = this.categoryMapper.selectByPrimaryKey(category2.getParentId());
        return Arrays.asList(category1,category2,category3);
    }


}
