package com.pinyougou.page.service;

/**
 * 商品详细页接口
 * @author xza
 * @date 2018/12/14 11:18
 */
public interface ItemPageService {
    boolean  genItemHtml(Long goodsId);
    /**
     * 删除商品详细页
     * @param
     * @return
     */
    public boolean deleteItemHtml(Long[] goodsIds);
}
