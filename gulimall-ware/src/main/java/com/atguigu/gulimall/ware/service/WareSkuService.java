package com.atguigu.gulimall.ware.service;

import com.atguigu.common.mq.StockLockedTo;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.to.ware.WareSkuLockTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author 无名氏
 * @email 2185180175@qq.com
 * @date 2022-04-18 22:22:59
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);


    void addOrUpdateStockBatchByskuIdAndwareId(Collection<PurchaseDetailEntity> purchaseDetailList);

    List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockTo wareSkuLockTo);


    void unLockStock(StockLockedTo stockLockedTo);

    void unLockStock(OrderTo orderTo);
}

