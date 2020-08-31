package com.leyou.item.controller;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;

import com.leyou.item.service.SpecificationService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    /**
     * 根据cid查询规格参数组
     * @param cid
     * @return
     */
    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupsByCid(@PathVariable("cid")Long cid){
        List<SpecGroup> specGroupList=this.specificationService.queryGroupsByCid(cid);
        if(CollectionUtils.isEmpty(specGroupList)){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(specGroupList);
    }

    /**
     * 根据组id 查询底下分组信息
     * @param gid
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParam>> querySpecParamByGid(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "generic",required = false)Boolean generric,
            @RequestParam(value = "search",required = false)Boolean searching
    ){
        List<SpecParam> specParamList=this.specificationService.querySpecParamByGid(gid,cid,generric,searching);
        if(CollectionUtils.isEmpty(specParamList)){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(specParamList);
    }

    @GetMapping("{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupsWirthParamByCid(@PathVariable("cid") Long cid){
        List<SpecGroup> list = this.specificationService.queryGroupsWirthParamByCid(cid);
        if(list == null || list.size() == 0){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(list);
    }


}
