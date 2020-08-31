package com.leyou.item.mapper;


import com.leyou.item.pojo.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CategoryMapper extends Mapper<Category> {

    /**
     * 根据品牌id查询分类
     * @param bid
     * @return
     */
    @Select("select * from tb_category where id in" +
            "(SELECT category_id FROM tb_category_brand WHERE brand_id=#{bid})")
    List<Category> selectCategoriesByBrandId(@Param("bid")Long bid);

    @Select("select * from tb_category where parent_id=#{pid}")
    List<Category> selectCategoriesByParentId(@Param("pid")Long pid);
}
