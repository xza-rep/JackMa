package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {

	
	/**
	 * 搜索方法
	 * @param searchMap
	 * @return
	 */
	Map<String, Object> search(Map searchMap);

	/**
	 * 将数据批量导入到索引库
	 * @param list
	 */
	public void importList(List list);

	/**
	 * 删除数据
	 * @param goodsIdsList
	 */
	public void deleteByGoodsIds(List goodsIdsList);
	
}
