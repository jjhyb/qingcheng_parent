package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.UserMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 返回全部记录
     * @return
     */
    public List<User> findAll() {
        return userMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<User> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<User> users = (Page<User>) userMapper.selectAll();
        return new PageResult<User>(users.getTotal(),users.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<User> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return userMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<User> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<User> users = (Page<User>) userMapper.selectByExample(example);
        return new PageResult<User>(users.getTotal(),users.getResult());
    }

    /**
     * 根据Id查询
     * @param username
     * @return
     */
    public User findById(String username) {
        return userMapper.selectByPrimaryKey(username);
    }

    /**
     * 新增
     * @param user
     */
    public void add(User user) {
        userMapper.insert(user);
    }

    /**
     * 修改
     * @param user
     */
    public void update(User user) {
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     *  删除
     * @param username
     */
    public void delete(String username) {
        userMapper.deleteByPrimaryKey(username);
    }

    /**
     * 发送短信验证码
     * @param phone 手机号
     */
    public void sendSms(String phone) {
        //1、生成6位的短信验证码
        Random random = new Random();
        int code = random.nextInt(899999)+100000;//生成短信验证码，保证生成6位
        logger.info("短信验证码："+code);

        //2、将验证码保存到redis中
        redisTemplate.boundValueOps("code_"+phone).set(code+"");
        redisTemplate.boundValueOps("code_"+phone).expire(5,TimeUnit.MINUTES);//5分钟过期
        //3、将验证码发送到rabbitmq
        Map<String,String> map = new HashMap();
        map.put("phone",phone);
        map.put("code",code+"");
        rabbitTemplate.convertAndSend("","queue.sms", JSON.toJSONString(map));
    }

    /**
     * 用户注册
     * @param user 用户
     * @param smsCode 短信验证码
     */
    public void add(User user, String smsCode) {
        //1、校验
        String sysCode = (String)redisTemplate.boundValueOps("code_" + user.getPhone()).get();//提取系统验证码
        if(StringUtils.isEmpty(sysCode)){
            throw new RuntimeException("验证码未发生，或已过期");
        }
        if(!sysCode.equals(smsCode)){
            throw new RuntimeException("验证码不匹配");
        }
        if(StringUtils.isEmpty(user.getUsername())){
            user.setUsername(user.getPhone());//把手机号作为用户名
        }
        //校验用户名是否已注册
        User searchUser = new User();
        searchUser.setUsername(user.getUsername());
        if(userMapper.selectCount(searchUser)>0){
            throw new RuntimeException("该手机号已注册");
        }
        //2、数据添加
        user.setCreated(new Date());//创建时间
        user.setUpdated(user.getCreated());//修改时间
        user.setStatus("1");//状态
        user.setIsEmailCheck("0");//邮箱验证
        user.setIsMobileCheck("1");//手机验证
        user.setPoints(0);//积分为0
        userMapper.insertSelective(user);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户名
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 密码，加密存储
            if(searchMap.get("password")!=null && !"".equals(searchMap.get("password"))){
                criteria.andLike("password","%"+searchMap.get("password")+"%");
            }
            // 注册手机号
            if(searchMap.get("phone")!=null && !"".equals(searchMap.get("phone"))){
                criteria.andLike("phone","%"+searchMap.get("phone")+"%");
            }
            // 注册邮箱
            if(searchMap.get("email")!=null && !"".equals(searchMap.get("email"))){
                criteria.andLike("email","%"+searchMap.get("email")+"%");
            }
            // 会员来源：1:PC，2：H5，3：Android，4：IOS
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 昵称
            if(searchMap.get("nickName")!=null && !"".equals(searchMap.get("nickName"))){
                criteria.andLike("nickName","%"+searchMap.get("nickName")+"%");
            }
            // 真实姓名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 使用状态（1正常 0非正常）
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }
            // 头像地址
            if(searchMap.get("headPic")!=null && !"".equals(searchMap.get("headPic"))){
                criteria.andLike("headPic","%"+searchMap.get("headPic")+"%");
            }
            // QQ号码
            if(searchMap.get("qq")!=null && !"".equals(searchMap.get("qq"))){
                criteria.andLike("qq","%"+searchMap.get("qq")+"%");
            }
            // 手机是否验证 （0否  1是）
            if(searchMap.get("isMobileCheck")!=null && !"".equals(searchMap.get("isMobileCheck"))){
                criteria.andLike("isMobileCheck","%"+searchMap.get("isMobileCheck")+"%");
            }
            // 邮箱是否检测（0否  1是）
            if(searchMap.get("isEmailCheck")!=null && !"".equals(searchMap.get("isEmailCheck"))){
                criteria.andLike("isEmailCheck","%"+searchMap.get("isEmailCheck")+"%");
            }
            // 性别，1男，0女
            if(searchMap.get("sex")!=null && !"".equals(searchMap.get("sex"))){
                criteria.andLike("sex","%"+searchMap.get("sex")+"%");
            }

            // 会员等级
            if(searchMap.get("userLevel")!=null ){
                criteria.andEqualTo("userLevel",searchMap.get("userLevel"));
            }
            // 积分
            if(searchMap.get("points")!=null ){
                criteria.andEqualTo("points",searchMap.get("points"));
            }
            // 经验值
            if(searchMap.get("experienceValue")!=null ){
                criteria.andEqualTo("experienceValue",searchMap.get("experienceValue"));
            }

        }
        return example;
    }

}
