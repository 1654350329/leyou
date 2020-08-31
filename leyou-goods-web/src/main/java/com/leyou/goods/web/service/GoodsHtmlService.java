package com.leyou.goods.web.service;

import com.leyou.common.unitls.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@Service
public class GoodsHtmlService {
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private GoodsService goodsService;
    //日志文件
    private static final Logger LOGGER = LoggerFactory.getLogger(GoodsHtmlService.class);

    public void createHtml(Long spuId){
        Context context = new Context();
        context.setVariables(this.goodsService.loadModel(spuId));

        PrintWriter printWriter=null;
        try {
            printWriter = new PrintWriter("D:\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
            // 执行页面静态化方法
            this.templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            LOGGER.error("页面静态化出错：{}，"+ e, spuId);
        }finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    /**
     * 新建线程处理页面静态化
     * @param spuId
     */
    public void asyncExcute(Long spuId) {
        ThreadUtils.execute(()->createHtml(spuId));
        /*ThreadUtils.execute(new Runnable() {
            @Override
            public void run() {
                createHtml(spuId);
            }
        });*/
    }

    public void deleteHtml(Long id) {
        File file = new File("D:\\nginx-1.14.0\\html\\item\\" + id + ".html");
        file.deleteOnExit();
    }
}
