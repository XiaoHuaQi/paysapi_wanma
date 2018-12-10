package com.zixu.paysapi.jpa.service;

import com.zixu.paysapi.jpa.entity.Commodity;
import com.zixu.paysapi.mvc.util.Page;

public interface CommodityService {
	
	Commodity save(Commodity commodity);
	
	Commodity findById(String id);
	
	Commodity findByName(String name,String userID);
	
	Commodity findByFee(int fee,String userID);
	
	Page<Commodity> findByPage(int pageNum,String name,String userID);
	
	void delete(String id);
}
