package com.pinyougou.cart.service;

import com.pinyougou.pojo.Cart;

import java.util.List;

/**购物车服务接口
 * @author xza
 * @date 2018/12/20 15:31
 */
public interface CartService {
    /**
     * 添加商品到购物车
     * @param cartList
     * @param itemId
     * @param num
     * @return
     */
    public List<Cart>  addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num);

    /**
     * 根据用户名从缓存中取出该用户的购物车列表
     * @param username
     * @return
     */
    public List<Cart> findCartListFromRedis(String username);

    /**
     * 将购物车列表存入到缓存中
     * @param username
     * @param cartList
     */
    public void saveCartListToRedis(String username,List<Cart> cartList);
    /**
     * 合并购物车
     * @param cartList1
     * @param cartList2
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart> cartList2);
}
