package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据cid获取分组
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup spec=new SpecGroup();
        spec.setCid(cid);
       return this.specGroupMapper.select(spec);
    }

    /**
     * 根据分组id获取据规格参数
     * @param gid
     * @return
     */
    public List<SpecParam> querySpecParamByGid(Long gid,Long cid,Boolean generic,Boolean searching) {
        SpecParam specParam =new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setGeneric(generic);
        specParam.setSearching(searching);
        return this.specParamMapper.select(specParam);
    }

    /**
     * 根据cid获取分组包括详情
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsWirthParamByCid(Long cid) {
        List<SpecGroup> groups = this.queryGroupsByCid(cid);
        groups.forEach(specGroup -> {
            List<SpecParam> params = this.querySpecParamByGid(specGroup.getId(), null, null, null);
            specGroup.setParams(params);
        });
        return groups;
    }

}
