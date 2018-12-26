package com.pinyougou.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.Cart;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**购物车服务实习类
 * @author xza
 * @date 2018/12/20 15:35
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private TbItemMapper itemMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
        //1.根据商品SKU ID查询SKU商品信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        if (item==null){ //该商品可能已售罄
            throw new RuntimeException("商品不存在");
        }
        if (!item.getStatus().equals("1")){
            throw new RuntimeException("商品状态无效");
        }
        //2.根据SKU获取商家ID
        String sellerId = item.getSellerId();
        //3.根据商家ID判断购物车列表是否存在该商家的购物车
        Cart cart=searchCartBySellerId(cartList,sellerId);
        if (cart==null){ //4.如果购物车列表中不存在该商家的购物车
            //4.1.新建购物车对象
            cart=new Cart();
            cart.setSellerId(sellerId);
            cart.setSellerName(item.getSeller());
            //创建购物明细对象
            TbOrderItem orderItem=createOrderItem(item,num);
            //创建商家购物车列表
            List orderItemList=new ArrayList();
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList);
            //4.2.将新建购物车对象添加到购物车列表
            cartList.add(cart);
        }else {//5.如果购物车列表中存在该商家的购物车
            //查询该购物车明细表是否存在该商品
            TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(),itemId);
            if(orderItem==null){
                //5.1.如果没有，新增购物车明细
              //  System.out.println("================");
                orderItem=createOrderItem(item,num);
                cart.getOrderItemList().add(orderItem);
            }else{
                //5.2.如果有，在原购物车明细上增加数量，修改金额
                orderItem.setNum(orderItem.getNum()+num);//增加数量
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum()*orderItem.getPrice().doubleValue()));//修改总金额
                if (orderItem.getNum()<=0){ //减少数量时，实际是加一个负数，所以可能为0
                   cart.getOrderItemList().remove(orderItem);//移除该明细对象
                }
                if(cart.getOrderItemList().size()==0){//如果该商家购车列表为空，删除该购物车对象
                    cartList.remove(cart);
                }
            }
        }
        return cartList;
    }

    @Override
    public List<Cart> findCartListFromRedis(String username) {
        System.out.println("从 redis 中提取购物车数据....."+username);
        //大key为CartList，小key为用户名，value为该用户的购物车列表
       List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
       if(cartList==null){
           cartList=new ArrayList<>();
       }
        return cartList;
    }

    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("向 redis 存入购物车数据....."+username);
    redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
        System.out.println("合并购物车");
        for (Cart cart : cartList2) {
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList1;
    }

    /**
     * 创建订单明细
     * @param item
     * @param num
     * @return
     */
    private TbOrderItem createOrderItem(TbItem item, Integer num) {
      if (num<0){
          throw new RuntimeException("数量非法");
      }

        TbOrderItem orderItem=new TbOrderItem();
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setItemId(item.getId());
        orderItem.setNum(num);
        orderItem.setPicPath(item.getImage());
        orderItem.setPrice(item.getPrice());
        orderItem.setSellerId(item.getSellerId());
        orderItem.setTitle(item.getTitle());
        orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue()*num));
      return orderItem;
    }

    /**
     * 根据商家ID查询购物车对象
     * @param cartList
     * @param sellerId
     * @return
     */
    private Cart searchCartBySellerId(List<Cart>cartList,String sellerId){
        for (Cart cart : cartList) {
            if (cart.getSellerId().equals(sellerId)){
                return cart;
            }
        }
        return null;
    }

    /**
     * 根据商品明细ID查询
     * @param orderItemList
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
        for (TbOrderItem orderItem : orderItemList) {
            if (orderItem.getItemId().longValue()==itemId.longValue()){ //如果购物车明细列表存在该明细对象，返回这个明细对象
                return orderItem;
            }
        }
        return null;
           }
}


