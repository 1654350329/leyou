package com.leyou.item.service;

import com.leyou.item.pojo.Category;

import java.util.List;


public interface CategoryService {
    /**
     * 根据父节点查询子节点
     * @param pid
     * @return
     */
    public List<Category> queryCategoriesByPid(Long pid) ;



    List<Category> queryCategoriesByBrandId(Long bid);

    List<String> queryNamesById(List<Long> asList);


    List<Category> queryCategoryByCid(Long cid);
}
