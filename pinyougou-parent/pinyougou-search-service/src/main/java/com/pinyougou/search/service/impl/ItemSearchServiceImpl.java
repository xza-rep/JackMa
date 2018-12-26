package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
@Service(timeout=90000)
public class ItemSearchServiceImpl implements ItemSearchService {

	@Autowired
	private SolrTemplate solrTemplate;

	@Autowired
	private RedisTemplate redisTemplate;


	@Override
	public Map<String, Object> search(Map searchMap) {
		Map<String, Object> map = new HashMap<String, Object>();

		//关键字空格处理
		String keywords= (String) searchMap.get("keywords");
		searchMap.put("keywords",keywords.replace(" ",""));
		//1.按关键字查询（高亮显示）
		map.putAll(searchList(searchMap));
	/*	Map map1 = (Map) map.get("spec");
		for (Object o : map1.keySet()) {
			System.out.println(o.toString());
		}*/
		//2.根据关键字查询商品分类
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList",categoryList);

		//3.查询品牌和规格列表
		String categoryName = (String) searchMap.get("category");
		if (!"".equals(categoryName)){
			//如果有分类名称
			map.putAll(searchBrandAndSpecList(categoryName));
		}else {//如果咩有分类名称
			if(categoryList.size()>0){
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}

		return map;
	}



    private Map searchList(Map<String,Object>searchMap){
		Map map=new HashMap();
		//高亮选项初始化
		SimpleHighlightQuery query = new SimpleHighlightQuery();
		HighlightOptions hos = new HighlightOptions();
		hos.setSimplePostfix("</em>"); //前缀
		hos.addField("item_title"); //高亮选项
		hos.setSimplePrefix("<em style='color:red'>"); //后缀
		query.setHighlightOptions(hos); //为查询设置高亮选项

		// 1.1关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);

		//1.2按分类筛选
		if(!"".equals(searchMap.get("category"))){
			Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category")); //筛选条件
			SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria); //构建条件查询对象
			query.addFilterQuery(filterQuery);//执行条件查询
		}

		//1.3按品牌筛选
		if(!"".equals(searchMap.get("brand"))){
			Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
			SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
			query.addFilterQuery(filterQuery);
		}

		//1.4按规格列表查询
		if(searchMap.get("spec")!=null){
			Map<String,String> specMap= (Map) searchMap.get("spec");
			for(String key:specMap.keySet() ){
				Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		//1.5按价格筛选
		if(!"".equals(searchMap.get("price"))){
			String[] price = ((String) searchMap.get("price")).split("-");
			if (!price[0].equals("0")){ //如果区间起点不等于0
				Criteria filterCriteria = new Criteria("item_price").greaterThan(price[0]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
			if (!price[1].equals("*")){ //如果区间起点不等于*
				Criteria filterCriteria = new Criteria("item_price").lessThan(price[1]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}

		//1.6分页查询
		Integer pageNo = (Integer)searchMap.get("pageNo"); //获取当前页码
		if(pageNo==null){
			pageNo=1; //默认为第一页
		}
		Integer pageSize = (Integer) searchMap.get("pageSize");//获取每页显示记录数
		if(pageSize==null){
			pageSize=20;//默认显示20条记录
		}

		query.setOffset((pageNo-1)*pageSize);//设置起始索引
		query.setRows(pageSize);//设置每页要显示的记录数
		
		//1.7排序
		String sortValue = (String) searchMap.get("sort"); //升序ASC，降序DESC
		String sortField = (String) searchMap.get("sortField"); //排序字段
		if(sortValue!=null&&!sortValue.equals("")){
			if(sortValue.equals("ASC")){//升序
				Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
				query.addSort(sort);
			}
			if(sortValue.equals("DESC")){//升序
				Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
				query.addSort(sort);
			}
		}

		//***********************获取高亮结果集****************//
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		List<HighlightEntry<TbItem>> highlighted = page.getHighlighted();
		for (HighlightEntry<TbItem> h : highlighted) {
			TbItem item = h.getEntity();// 获取原实体类
			if (h.getHighlights().size() > 0 && h.getHighlights().get(0).getSnipplets().size() > 0) {
				item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));// 设置高亮的结果
			}
		}
		map.put("rows", page.getContent());
		map.put("totalPages",page.getTotalPages());//返回总页数
		map.put("total",page.getTotalPages());//返回总记录数
		return map;
	}

	/**
	 * 分组查询商品分类
	 * @param searchMap
	 * @param
	 */
	private List searchCategoryList(Map searchMap){
		List<String> list=new ArrayList<>();
		Query query=new SimpleQuery();
		//添加查询条件（根据关键字进行查询）
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords")); //相当于where
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");//按分类进行分组
		query.setGroupOptions(groupOptions);//拼接到查询语句上

		//可能按照多个域进行分组，所以会得到不同的分组结果
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);//分组页
		//根据列得到分组结果集（得到品牌分类分组的结果）
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//得到分组结果入口页，将分组结果再进行分页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		//得到分组入口集合(得到每一条记录）
		List<GroupEntry<TbItem>> content = groupEntries.getContent();
		for (GroupEntry<TbItem> entry : content) {
			list.add(entry.getGroupValue());//将分组的结果名称封装到返回值中
		}
        return list;

	}

	/**
	 * 查询品牌和规格列表
	 * @param category 分类名称
	 * @return
	 */
	private Map searchBrandAndSpecList(String category){
		Map map=new HashMap();
		Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
		if(typeId!=null){
			//根据模板 ID 查询品牌列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);//返回值添加品牌列表
           //根据模板 ID 查询规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);
		}
		return map;
	}

    /**
     * 将审核的商品添加到索引库
     * @param list
     */
    @Override
    public void importList(List list) {
     solrTemplate.saveBeans(list);
     solrTemplate.commit();
    }

	@Override
	public void deleteByGoodsIds(List goodsIdsList) {
		System.out.println("删除商品ID"+goodsIdsList);
		SimpleQuery query = new SimpleQuery();
		Criteria criteria = new Criteria("item_goodsid").in(goodsIdsList);
		query.addCriteria(criteria);
		solrTemplate.delete(query);
		solrTemplate.commit();
	}
}
