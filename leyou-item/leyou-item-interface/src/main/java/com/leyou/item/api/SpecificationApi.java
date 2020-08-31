package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@RequestMapping("spec")
public interface SpecificationApi {
    /**
     * 根据cid查询规格参数组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
     List<SpecGroup> queryGroupsByCid(@PathVariable("cid")Long cid);

    /**
     * 根据组id 查询底下分组信息
     * @param gid
     * @return
     */
    @GetMapping("params")
     List<SpecParam> querySpecParamByGid(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "generic",required = false)Boolean generric,
            @RequestParam(value = "search",required = false)Boolean searching
    );

    /**
     * 根据组id查询包括底下分组信息
     * @param cid
     * @return
     */
    @GetMapping("{cid}")
    List<SpecGroup> queryGroupsWirthParamByCid(@PathVariable("cid") Long cid);

}
