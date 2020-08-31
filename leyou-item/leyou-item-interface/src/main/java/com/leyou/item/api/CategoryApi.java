package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@RequestMapping("category")
public interface CategoryApi {
    /**
     * 根据cid查找分类名称
     * @param ids
     * @return
     */
    @GetMapping("names")
     List<String> queryNamesByIds(@RequestParam("ids")List<Long> ids);

    /**
     * 根据3级分类id，查询1~3级的分类
     * @param cid
     * @return
     */
    @GetMapping("all/level")
    List<Category> queryCategoryByCid(@RequestParam("cid")Long cid);
}
