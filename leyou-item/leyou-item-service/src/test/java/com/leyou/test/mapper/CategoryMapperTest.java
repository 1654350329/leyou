package com.leyou.test.mapper;

import com.leyou.item.LeyouItemServiceApplication;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Stock;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LeyouItemServiceApplication .class)
public class CategoryMapperTest {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Test
    public void delete(){
        this.stockMapper.deleteByPrimaryKey(27359021736L);
    }

    @Test
    public void queryNamesById() {
        List<String> stringList=new ArrayList<>();
        Category category = this.categoryMapper.selectByPrimaryKey(3);

        System.out.println(category);
    }
}
