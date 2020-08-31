package com.leyou.search.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;


import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.reponsitory.GoodsRepository;
import org.apache.catalina.mapper.Mapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.search.MultiMatchQuery;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;



    //mapper工具map转json
    private ObjectMapper MAPPER = new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();
        //根据cid获取分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        //获取品牌名称
        Brand brand = this.brandClient.queryBrandByBid(spu.getBrandId());

        //获取sku且得到价格
        List<Sku> skus = this.goodsClient.querySkusBySid(spu.getId());
        List<Long> price = new ArrayList<>();
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            price.add(sku.getPrice());
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("price", sku.getPrice());
            map.put("title", sku.getTitle());
            //取第一张图
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMapList.add(map);
        });


        //获取搜索通用字段
        List<SpecParam> params = this.specificationClient.querySpecParamByGid(null, spu.getCid3(), null, true);
        //获取spu详情
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySid(spu.getId());
        //通用规格参数反序列化
        Map<Long, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<Long, Object>>() {
        });
        //特殊规格参数反序列化
        Map<Long, List<Object>> SpecialSpecgMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<Object>>>() {
        });

        //搜索字段赋值
        Map<String, Object> specParamMap = new HashMap<>();
        params.forEach(param -> {
            if (param.getGeneric() == true) {
                String value = genericSpecMap.get(param.getId()).toString();
                //根据值返回区间
                if (param.getNumeric()) {
                    value = chooseSegment(value, param);
                }
                specParamMap.put(param.getName(), value);
            } else {
                List<Object> value = SpecialSpecgMap.get(param.getId());

                specParamMap.put(param.getName(), value);
            }
        });


        //设置goods信息
        goods.setId(spu.getId());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //标题+分类+品牌
        goods.setAll(spu.getSubTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        //价格
        goods.setPrice(price);
        //String skus;// List<sku>信息的json结构
        //Map<String, Object> specs;// 可搜索的规格参数，key是参数名，值是参数值
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(specParamMap);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {
        String key = request.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //基本查询条件
       // QueryBuilder baseQuery = QueryBuilders.matchQuery("all", key).operator(Operator.AND);
        BoolQueryBuilder boolQueryBuilder = buildBooleanQueryBuilder(request);
        // 1、对key进行全文检索查询
        queryBuilder.withQuery(boolQueryBuilder);

        // 2、通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(
                new String[]{"id", "skus", "subTitle"}, null));

        // 3、分页
        // 准备分页参数
        int page = request.getPage();
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page - 1, size));

        String sortBy = request.getSortBy();
        Boolean descending = request.getDescending();
        if(StringUtils.isNotBlank(sortBy)){
            //如果不为空则排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(descending? SortOrder.ASC:SortOrder.DESC));
        }

        String categoryAggName = "categories";
        String brandAggName = "brands";
        //根据品牌Id 聚合查询
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //根据分类名称cid3 聚合查询
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));


        // 4、查询，获取结果
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // 解析聚合结果集
        List<Map<String, Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));
        // 判断分类聚合的结果集大小，等于1则聚合
        List<Map<String, Object>> specs = null;
        if (categories.size()==1){
             specs= getSpecsAggResult((Long)categories.get(0).get("id"),boolQueryBuilder);
        }
      //  System.out.println(specs.get(1));
        // 封装结果并返回
        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(),categories,brands,specs);
    }

    /**
     * 聚合出规格参数过滤条件
     * @param id
     * @param baseQuery
     * @return
     */
    private List<Map<String, Object>> getSpecsAggResult(Long id, QueryBuilder baseQuery) {
        //自定义查询对象构建
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加查询条件
        queryBuilder.withQuery(baseQuery);
        //根据cid查询规格参数
        List<SpecParam> params = this.specificationClient.querySpecParamByGid(null, id, null, true);
        //添加结果集聚合
        params.forEach(param ->{
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs."+param.getName()+".keyword"));
        });
        // 只需要聚合结果集，不需要查询结果集
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

        //查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        //解析聚合结果集
        // 处理聚合结果集
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        //构建规格参数对象
        List<Map<String, Object>> specs=new ArrayList<>();
        //通过Map.entrySet遍历key和value
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            //收集规格参数
            Map<String, Object> map=new HashMap<>();
            //放入规格参数名
            map.put("k",entry.getKey());
            //收集规格参数值
            List<Object> options=new ArrayList<>();
            //解析每个聚合
            StringTerms terms= (StringTerms) entry.getValue();
            //遍历每个聚合中桶
            terms.getBuckets().forEach(bucket -> options.add(bucket.getKeyAsString()));

            map.put("options",options);
            specs.add(map);
        }
        return  specs;
    }
    /**
     * 构建bool查询构建器
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBooleanQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));

        // 添加过滤条件
        if (CollectionUtils.isEmpty(request.getFilter())){
            return boolQueryBuilder;
        }
        for (Map.Entry<String, Object> entry : request.getFilter().entrySet()) {

            String key = entry.getKey();
            // 如果过滤条件是“品牌”, 过滤的字段名：brandId
            if (StringUtils.equals("品牌", key)) {
                key = "brandId";
            } else if (StringUtils.equals("分类", key)) {
                // 如果是“分类”，过滤字段名：cid3
                key = "cid3";
            } else {
                // 如果是规格参数名，过滤字段名：specs.key.keyword
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }

        return boolQueryBuilder;
    }

    /**
     * 解析品牌聚合结果集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        // 处理聚合结果集
        LongTerms terms = (LongTerms) aggregation;
        // 获取所有的品牌id桶
        List<LongTerms.Bucket> buckets = terms.getBuckets();
        // 定义一个品牌集合，搜集所有的品牌对象
        List<Brand> brands = new ArrayList<>();
        // 解析所有的id桶，查询品牌
        buckets.forEach(bucket -> {
            Brand brand = this.brandClient.queryBrandByBid(bucket.getKeyAsNumber().longValue());
            brands.add(brand);
        });
        return brands;
    }

    /**
     * 解析分类
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation) {
        // 处理聚合结果集
        LongTerms terms = (LongTerms)aggregation;
        // 获取所有的分类id桶
        List<LongTerms.Bucket> buckets = terms.getBuckets();
        // 定义一个品牌集合，搜集所有的品牌对象
        List<Map<String, Object>> categories = new ArrayList<>();
        List<Long> cids = new ArrayList<>();
        // 解析所有的id桶，查询品牌
        buckets.forEach(bucket -> {
            cids.add(bucket.getKeyAsNumber().longValue());
        });
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        return categories;
    }


    public void createIndex(Long id) throws IOException {
        //获取spu信息
        Spu spu = this.goodsClient.querySpuById(id);
        //重新构建商品
        Goods goods = this.buildGoods(spu);
        // 保存数据到索引库
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long id) {
        this.goodsRepository.deleteById(id);
    }
}

