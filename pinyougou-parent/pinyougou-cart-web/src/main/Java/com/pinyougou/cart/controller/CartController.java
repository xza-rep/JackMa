package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojo.Cart;
import entity.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import util.CookieUtil;

import java.util.List;

/**
 * @author xza
 * @date 2018/12/20 20:41
 */
@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference
    private CartService cartService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    /**
     * 购物车列表
     * @return
     */
    @RequestMapping("/findCartList")
   // @CrossOrigin(origins="http://localhost:9105",allowCredentials="true")//该注解spring必须是4.2版本以上才能使用
    public List<Cart> findCartList(){
        //得到登陆人账号,判断当前是否有人登陆
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        response.setHeader("Access-Control-Allow-Origin","http://localhost:9105");////可以访问的域(当此方法不需要操作cookie)
        response.setHeader("Access-Control-Allow-Credentials","true");//如果操作cookie，必须加上这句话
       //不管登录与否，都是先从本地cookie中查找
        String cartListString = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
        if (cartListString==null||cartListString.equals("")){
            cartListString="[]";
        }
        List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);
        if(name.equals("anonymousUser")){
            //没有登录，从cookie中提取购物车
            System.out.println("从cookie中提取购物车");
            return  cartList_cookie;
        }else {
            //已经登录，从redis中取
            System.out.println("从redis中提取购物车");
            List<Cart> cartList_redis = cartService.findCartListFromRedis(name);
            if(cartList_cookie.size()>0){
                //合并购物车
                cartList_redis = cartService.mergeCartList(cartList_cookie, cartList_redis);
                //清除本地cookie的数据
                util.CookieUtil.deleteCookie(request,response,"cartList");
                //将合并后的数据存入redis
                cartService.saveCartListToRedis(name,cartList_redis);
            }
            return cartList_redis;
        }

    }

    /**
     * 添加商品到购物车
     * @param itemId
     * @param num
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(Long itemId,Integer num){
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录用户： "+name);
        try{
            //1.取出购物车
            List<Cart> cartList = findCartList();
            //2.调用服务层操作购物车
            cartList = cartService.addGoodsToCartList(cartList,itemId, num);
            if(name.equals("anonymousUser")){//没有登录
                //将购物车存入到cookie
                util.CookieUtil.setCookie(request,response,"cartList",JSON.toJSONString(cartList),3600*24,"UTF-8");
                System.out.println("向 cookie 存入数据");
            }else{
                //将购物车存入redis
                cartService.saveCartListToRedis(name,cartList);
                System.out.println("向redis存储购物车");
            }
            return new Result(true,"添加购物车成功");
        }catch (Exception e){
            e.printStackTrace();
            return new Result(true,"添加购物车失败");
        }
    }
}
