package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDatailMapper spuDatailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    //日志文件
    private static final Logger LOGGER= LoggerFactory.getLogger(GoodsService.class);

    public PageResult<SpuBo> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows) {
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索条件
        if (StringUtils.isNotBlank(key)){

            criteria.andLike("title","%"+key+"%");
        }
       if (saleable!=null){
           criteria.andEqualTo("saleable",saleable);
       }
       //分页条件
        PageHelper.startPage(page,rows);
        //执行查询
        List<Spu> spus = this.spuMapper.selectByExample(example);
        PageInfo<Spu> pageInfo=new PageInfo<Spu>();
        List<SpuBo> spuBos=new ArrayList<>();
        spus.forEach(spu -> {
            SpuBo spuBo=new SpuBo();
            // copy共同属性的值到新的对象
            BeanUtils.copyProperties(spu,spuBo);
            //查询品牌名称
            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());

            //查询分类名称
             List<String> names = this.categoryService.queryNamesById(Arrays.asList(spu.getCid1(),spu.getCid2(),spu.getCid3()));
            //数组转文字
            spuBo.setCname(StringUtils.join(names,"/"));
            spuBos.add(spuBo);
        });
        return new PageResult<>(pageInfo.getTotal(), spuBos);
    }

    @Transactional
    public void saveGoods(SpuBo spuBo) {
        //新增Spu
        spuBo.setId(null);
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        this.spuMapper.insertSelective(spuBo);

        //新增SpuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        this.spuDatailMapper.insertSelective(spuDetail);
        //新增规格参数
        saveSkuAndStock(spuBo);

        sendMessage(spuBo.getId(),"insert");
    }

    //新增规格参数
    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku -> {
            // 新增sku
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insertSelective(sku);

            // 新增库存
            Stock stock = new Stock();
            stock.setSeckillStock(null);
            stock.setSeckillTotal(null);
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            this.stockMapper.insertSelective(stock);
        });

    }

    /**
     * 根据sid查找spu
     * @param id
     * @return
     */
    public SpuDetail querySpuDetailBySid(Long id) {
       return this.spuDatailMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据spu_id查找sku
     * @param spuid
     * @return
     */
    public List<Sku> querySkusBySid(Long spuid) {
        Sku sku=new Sku();
        sku.setSpuId(spuid);
        List<Sku> skus = this.skuMapper.select(sku);
        skus.forEach(s -> {
            Stock stock = this.stockMapper.selectByPrimaryKey(s.getId());
            s.setStock(stock.getStock());
        });
        return skus;
    }

    /**
     * 更新商品信息
     * @param spuBo
     */
    @Transactional
    public void updateGoods(SpuBo spuBo) {
        //更新spu信息
        spuBo.setLastUpdateTime(new Date());
        this.spuMapper.updateByPrimaryKeySelective(spuBo);

        // 更新spu详情
        this.spuDatailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());

        //因为也许sku已经删除  所以要先删除存在的sku再增加
        List<Sku> skus = this.querySkusBySid(spuBo.getId());
        if(!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku -> {
                this.skuMapper.deleteByPrimaryKey(sku.getId());
                this.stockMapper.deleteByPrimaryKey(sku.getId());
            });
        }
        //重新新增sku和库存
        this.saveSkuAndStock(spuBo);
        sendMessage(spuBo.getId(),"update");

    }


    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    private void sendMessage(Long id, String type){
        // 发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (Exception e) {
            LOGGER.error("{}商品消息发送异常，商品id：{}", type, id, e);
        }
    }

    public Sku querySkuById(Long id) {
        return this.skuMapper.selectByPrimaryKey(id);
    }
}
