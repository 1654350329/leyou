package com.leyou.goods.web.service;

import com.leyou.goods.web.client.BrandClient;
import com.leyou.goods.web.client.CategoryClient;
import com.leyou.goods.web.client.GoodsClient;
import com.leyou.goods.web.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;

    public Map<String, Object> loadModel(Long spu_id){
        Map<String, Object> map = new HashMap<>();

        Spu spu = this.goodsClient.querySpuById(spu_id);
        //获取各级分类名称且只保留姓名与id
        List<Category> categories = this.categoryClient.queryCategoryByCid(spu.getCid3());
        List<Map<String, Object>> categoryMaps=new ArrayList<>();
        categories.forEach(category -> {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("name",category.getName());
            categoryMap.put("id",category.getId());
            categoryMaps.add(categoryMap);
        });

        //获取品牌名称
        Brand brand = this.brandClient.queryBrandByBid(spu.getBrandId());
        
        //获取sku信息
        List<Sku> skus = this.goodsClient.querySkusBySid(spu_id);

        //获取spuDetail信息
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySid(spu_id);

        //获取规格参数组
        List<SpecGroup> groups = this.specificationClient.queryGroupsWirthParamByCid(spu.getCid3());

        //获取规格参数
        List<SpecParam> params = this.specificationClient.querySpecParamByGid(null, spu.getCid3(), false, null);
        //封装规格参数 只要名字和id
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });
        //封装spu
        map.put("spu",spu);
        // 封装spuDetail
        map.put("spuDetail", spuDetail);
        //封装分类
        map.put("categoryMaps",categoryMaps);
        // 封装sku集合
        map.put("skus", skus);
        // 品牌
        map.put("brand", brand);
        // 规格参数组
        map.put("groups", groups);
        // 查询特殊规格参数
        map.put("paramMap", paramMap);
        return map;
    }
}
