package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.PreferentialMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Preferential;
import com.qingcheng.service.order.PreferentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class PreferentialServiceImpl implements PreferentialService {

    @Autowired
    private PreferentialMapper preferentialMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Preferential> findAll() {
        return preferentialMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Preferential> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Preferential> preferentials = (Page<Preferential>) preferentialMapper.selectAll();
        return new PageResult<Preferential>(preferentials.getTotal(),preferentials.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Preferential> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return preferentialMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Preferential> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Preferential> preferentials = (Page<Preferential>) preferentialMapper.selectByExample(example);
        return new PageResult<Preferential>(preferentials.getTotal(),preferentials.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Preferential findById(Integer id) {
        return preferentialMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param preferential
     */
    public void add(Preferential preferential) {
        preferentialMapper.insert(preferential);
    }

    /**
     * 修改
     * @param preferential
     */
    public void update(Preferential preferential) {
        preferentialMapper.updateByPrimaryKeySelective(preferential);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(Integer id) {
        preferentialMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据分类和消费查询优惠金额
     * @param categoryId 分类id
     * @param money 消费金额
     * @return
     */
    @Override
    public int findPreMoneyByCategoryId(Integer categoryId, int money) {
        //指定查询条件在优惠规则计算表中查询
        //指定的查询条件：状态：1 、 分类：  、消费额 、 开始时间和截止时间 、 排序：消费额降序
        //比如：满200减40、满150减25   那么消费201应该取出的优惠金额为40

        Example example = new Example(Preferential.class);
        example.createCriteria().andEqualTo("state","1")//状态
                .andEqualTo("categoryId",categoryId)//分类
                .andLessThanOrEqualTo("buyMoney",money)//消费额条件，查询的表里buyMoney小于等于money
                .andGreaterThanOrEqualTo("endTime",new Date())//截止日期大于等于当前日期
                .andLessThanOrEqualTo("startTime",new Date());//开始日期小于等于当前日期
        example.setOrderByClause("buy_money DESC");//按购买金额降序排序
        List<Preferential> preferentialList = preferentialMapper.selectByExample(example);
        if(!CollectionUtils.isEmpty(preferentialList)){
            //有优惠
            Preferential preferential = preferentialList.get(0);
            if("1".equals(preferential.getType())){//如果满减金额不翻倍
                return preferential.getPreMoney();
            }else{//如果满减金额翻倍
                //计算消费金额是满减金额的倍数
                int multiple = money / preferential.getBuyMoney();//消费金额是满减金额的倍数
                return preferential.getPreMoney() * multiple;
            }
        }else {
            //没有优惠
            return 0;
        }
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Preferential.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 状态
            if(searchMap.get("state")!=null && !"".equals(searchMap.get("state"))){
                criteria.andLike("state","%"+searchMap.get("state")+"%");
            }
            // 类型1不翻倍 2翻倍
            if(searchMap.get("type")!=null && !"".equals(searchMap.get("type"))){
                criteria.andLike("type","%"+searchMap.get("type")+"%");
            }

            // ID
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }
            // 消费金额
            if(searchMap.get("buyMoney")!=null ){
                criteria.andEqualTo("buyMoney",searchMap.get("buyMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 品类ID
            if(searchMap.get("categoryId")!=null ){
                criteria.andEqualTo("categoryId",searchMap.get("categoryId"));
            }

        }
        return example;
    }

}
