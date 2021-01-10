package com.leyou.order.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.auth.entity.UserInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.IdWorker;
import com.leyou.order.interceptor.LoginInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.StockMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;
    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;//无法添加分布式锁

    @Autowired
    private Redisson redisson;//redisson 可以添加分布式锁

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final String lockKey="lockKey";
    @Transactional
    public Long createOrder(Order order) {
        // 生成orderId
        long orderId = idWorker.nextId();
        long time = new Date().getTime();
        // 获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
//        order.getOrderDetails().forEach(orderDetail -> {

//            Stock stock = this.stockMapper.selectByPrimaryKey(orderDetail.getSkuId());
//            if (stock.getStock()==0 ||stock.getStock()<0){
//                return;
//            }
//        });
        //this.redisTemplate.opsForSet().add("","");
        String clientID = UUID.randomUUID().toString();
        this.redisTemplate.opsForValue().setIfAbsent(lockKey,clientID);//jedis.setnx() 如果值存在不修改 不存才可以修改
        this.redisTemplate.expire(lockKey,30, TimeUnit.SECONDS);//设置过期时间30s
        //添加分布式锁
        RLock redissonLock = this.redisson.getLock(clientID);
        try {
            //加锁
            redissonLock.lock();//最后要解锁

            // 初始化数据
            order.setBuyerNick(user.getUsername());
            order.setBuyerRate(false);
            order.setCreateTime(new Date());
            order.setOrderId(orderId);
            order.setUserId(user.getId());
            // 保存数据
            this.orderMapper.insertSelective(order);

            // 保存订单状态
            OrderStatus orderStatus = new OrderStatus();
            orderStatus.setOrderId(orderId);
            orderStatus.setCreateTime(order.getCreateTime());
            orderStatus.setStatus(1);// 初始状态为未付款

            this.statusMapper.insertSelective(orderStatus);

            // 订单详情中添加orderId
            order.getOrderDetails().forEach(od -> od.setOrderId(orderId));
            // 保存订单详情,使用批量插入功能
            this.detailMapper.insertList(order.getOrderDetails());

            logger.debug("生成订单，订单编号：{}，用户id：{}", orderId, user.getId());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redissonLock.unlock();
        }
        return orderId;
    }

    public Order queryById(Long id) {
        // 查询订单
        Order order = this.orderMapper.selectByPrimaryKey(id);

        // 查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(id);
        List<OrderDetail> details = this.detailMapper.select(detail);
        order.setOrderDetails(details);

        // 查询订单状态
        OrderStatus status = this.statusMapper.selectByPrimaryKey(order.getOrderId());
        order.setStatus(status.getStatus());
        return order;
    }

    public PageResult<Order> queryUserOrderList(Integer page, Integer rows, Integer status) {
        try {
            // 分页
            PageHelper.startPage(page, rows);
            // 获取登录用户
            UserInfo user = LoginInterceptor.getLoginUser();
            // 创建查询条件
            Page<Order> pageInfo = (Page<Order>) this.orderMapper.queryOrderList(user.getId(), status);

            return new PageResult<>(pageInfo.getTotal(), pageInfo);
        } catch (Exception e) {
            logger.error("查询订单出错", e);
            return null;
        }
    }

    @Transactional
    public Boolean updateStatus(Long id, Integer status) {
        OrderStatus record = new OrderStatus();
        record.setOrderId(id);
        record.setStatus(status);
        // 根据状态判断要修改的时间
        switch (status) {
            case 2:
                record.setPaymentTime(new Date());// 付款
                break;
            case 3:
                record.setConsignTime(new Date());// 发货
                break;
            case 4:
                record.setEndTime(new Date());// 确认收获，订单结束
                break;
            case 5:
                record.setCloseTime(new Date());// 交易失败，订单关闭
                break;
            case 6:
                record.setCommentTime(new Date());// 评价时间
                break;
            default:
                return null;
        }
        int count = this.statusMapper.updateByPrimaryKeySelective(record);
        return count == 1;
    }

}
