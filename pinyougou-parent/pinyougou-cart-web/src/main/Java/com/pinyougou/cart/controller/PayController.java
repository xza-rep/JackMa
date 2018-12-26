package com.pinyougou.cart.controller;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;

import com.pinyougou.pojo.TbPayLog;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import util.IdWorker;

import java.util.HashMap;
import java.util.Map;

/**微信支付控制层
 * @author xza
 * @date 2018/12/22 16:05
 */
@RestController
@RequestMapping("/pay")
public class PayController {
    @Reference
    private WeixinPayService weixinPayService;
    @Reference
    private OrderService orderService;
    /**
     * 生成二维码
     * @return
     */
    @RequestMapping("/createNative")
    public Map createNative(){
        //获取当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //到redis查询支付日志
        TbPayLog payLog = orderService.searchPayLogFromRedis(username);
        //判断支付日志存在
        if(payLog!= null){
            return weixinPayService.createNative(payLog.getOutTradeNo(),payLog.getTotalFee()+"");
        }else {
            return new HashMap();
        }
    }

    /**
     * 查询支付状态
     * @param out_trade_no
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no){
        Result result=null;
        int x=0;
        while (true){
            Map<String,String> payStatus = weixinPayService.queryPayStatus(out_trade_no);
            if (payStatus==null){
                result=new Result(false,"支付出现错误");
                break;
            }
            if (payStatus.get("trade_state").equals("SUCCESS")){
                result=new Result(true,"支付成功");
                //修改订单状态
                orderService.updateOrderStatus(out_trade_no,payStatus.get("transaction_id"));
                break;
            }
            /**
             * SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付
             CLOSED—已关闭 REVOKED—已撤销（付款码支付）USERPAYING--用户支付中（付款码支付）
             PAYERROR--支付失败(其他原因，如银行返回失败)
             */
            try {
                Thread.sleep(3000);//间隔三秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            x++;
            if (x>4){
                System.out.println("aaa");
                result=new Result(false, "二维码超时");
                break;//跳出循环，不再执行，因为可能上面两个判断都没有走，那么就是死循环，所以加上这个判断
            }
        }
        System.out.println(result.isFlag()+result.getMessage());
        return result;
    }
}
