package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    AttrGroupService attrGroupService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    /**
     * 根据条件分页查询
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * key: '华为',//检索关键字
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     * }
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SkuInfoEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //根据"key"，精确匹配商品id 或 模糊查询spu_name
        String key = (String) params.get("key");
        lambdaQueryWrapper.and(StringUtils.hasLength(key)  && !"0".equalsIgnoreCase(key), wrapper -> {
            wrapper.eq(SkuInfoEntity::getSkuId, key).or().like(SkuInfoEntity::getSkuName, key);
        });

        //根据catelogId精确匹配所属分类id（注意：前端发来的是catelogId,数据库写的是catalogId）
        String catelogId = (String) params.get("catelogId");
        lambdaQueryWrapper.eq(StringUtils.hasLength(catelogId) && !"0".equalsIgnoreCase(catelogId), SkuInfoEntity::getCatalogId, catelogId);

        //根据brandId精确匹配品牌id
        String brandId = (String) params.get("brandId");
        lambdaQueryWrapper.eq(StringUtils.hasLength(brandId) && !"0".equalsIgnoreCase(brandId), SkuInfoEntity::getBrandId, brandId);

        // price >= min
        String min = (String) params.get("min");
        if (StringUtils.hasLength(min)) {
            try {
                BigDecimal mimBigDecimal = new BigDecimal(min);
                lambdaQueryWrapper.ge(mimBigDecimal.compareTo(BigDecimal.ZERO)>0, SkuInfoEntity::getPrice, min);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // price <= max
        String max = (String) params.get("max");
        if (StringUtils.hasLength(max)) {
            try {
                BigDecimal maxBigDecimal = new BigDecimal(max);
                lambdaQueryWrapper.le( maxBigDecimal.compareTo(BigDecimal.ZERO)>0, SkuInfoEntity::getPrice, max);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                lambdaQueryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        LambdaQueryWrapper<SkuInfoEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SkuInfoEntity::getSpuId,spuId);
        List<SkuInfoEntity> list = this.list(lambdaQueryWrapper);
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        //1、sku基本信息获取pms_sku_info
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfoEntity = this.getById(skuId);
            skuItemVo.setInfo(skuInfoEntity);
            return skuInfoEntity;
        }, executor);
        //3、获取spu的销售属性组合。
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((info) -> {
            List<SkuItemVo.SkuItemSaleAttrVo> skuItemSaleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(info.getSpuId());
            skuItemVo.setSaleAttr(skuItemSaleAttrVos);
        },executor);
        //4、获取spu的介绍 pms_spu_info_desc
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((info) -> {
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(info.getSpuId());
            skuItemVo.setDesp(spuInfoDescEntity);
        }, executor);
        //5、获取spu的规格参数信息。
        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((info) -> {
            List<SkuItemVo.SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuIdAndCatalogId(info.getSpuId(), info.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);

        //2、sku的图片信息pms_sku_images
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        },executor);

        //不用添加infoFuture，因为 saleAttrFuture,descFuture,baseAttrFuture能完成在其之前的infoFuture必然能完成
        CompletableFuture.allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture).get();
        return skuItemVo;
    }

}