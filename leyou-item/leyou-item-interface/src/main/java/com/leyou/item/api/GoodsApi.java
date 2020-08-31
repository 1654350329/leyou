package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;



public interface GoodsApi {
    /**
     * 根据spuid查找sku
     * @param id
     * @return
     */
    @GetMapping("sku/list")
     List<Sku> querySkusBySid(@RequestParam("id")Long id);


    /**
     * 查找spu详细
     * @param sid
     * @return
     */
    @GetMapping("spu/detail/{sid}")
     SpuDetail querySpuDetailBySid(@PathVariable("sid")Long sid);

    @GetMapping("spu/page")
     PageResult<SpuBo> page(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "saleable",required = false)Boolean saleable,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows
    );
    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
     Spu querySpuById(@PathVariable("id") Long id);

    /**
     * 根据id查询sku
     * @param id
     * @return
     */
    @GetMapping("sku/{id}")
    public Sku querySkuById(@PathVariable("id")Long id);
}
