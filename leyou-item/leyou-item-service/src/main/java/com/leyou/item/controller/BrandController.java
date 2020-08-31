package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 根据查询条件分页并排序查询品牌信息
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     */
    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> Page(@RequestParam(value = "key",required = false)String key,
                               @RequestParam(value = "page",defaultValue = "1")Integer page,
                               @RequestParam(value = "rows",defaultValue = "5")Integer rows,
                               @RequestParam(value = "sortBy",required = false)String sortBy,
                               @RequestParam(value = "desc",required = false)Boolean desc)
    {
        PageResult<Brand> result= this.brandService.queryBrandByPage(key,page,rows,sortBy,desc);
        if (CollectionUtils.isEmpty(result.getItems())){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("cids") List<Long> cids){
        this.brandService.saveBrand(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据cid查找品牌
     * @param cid
     * @return
     */
    @GetMapping("cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandByCid(@PathVariable("cid")Long cid){
       List<Brand> brands= this.brandService.queryBrandByCid(cid);
       if(CollectionUtils.isEmpty(brands)){
           return ResponseEntity.notFound().build();
       }
       return ResponseEntity.ok(brands);
    }

    /**
     * 根据bid查找品牌
     * @param bid
     * @return
     */
    @GetMapping("bid/{bid}")
    public ResponseEntity<Brand> queryBrandByBid(@PathVariable("bid")Long bid){
        Brand brand= this.brandService.queryBrandByBid(bid);
        if(brand==null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(brand);
    }
}
