package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("brand")
public interface BrandApi {
    /**
     * 根据bid查找品牌
     * @param bid
     * @return
     */
    @GetMapping("bid/{bid}")
    public Brand queryBrandByBid(@PathVariable("bid")Long bid);
}
