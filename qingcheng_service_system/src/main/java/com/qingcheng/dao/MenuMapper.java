package com.qingcheng.dao;

import com.qingcheng.pojo.system.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface MenuMapper extends Mapper<Menu> {

    /**
     * 根据用户名查询用户菜单，包括商品类名的1级、2级、3级
     * 实际开发不要使用这种方式，因为子查询，效率很慢，实际可以使用隐式内连或左外连接
     * @param username
     * @return
     */
    @Select("SELECT * FROM tb_menu WHERE id IN ( " +
            "SELECT menu_id FROM tb_resource_menu  WHERE resource_id IN( " +
            "SELECT resource_id FROM tb_role_resource WHERE role_id IN( " +
            "SELECT role_id FROM tb_admin_role WHERE admin_id IN( " +
            "SELECT id FROM tb_admin WHERE login_name= #{username})))) " +
            "UNION " +
            "SELECT * FROM tb_menu WHERE id IN ( " +
            "SELECT parent_id FROM tb_menu WHERE id IN ( " +
            "SELECT menu_id FROM tb_resource_menu  WHERE resource_id IN( " +
            "SELECT resource_id FROM tb_role_resource WHERE role_id IN( " +
            "SELECT role_id FROM tb_admin_role WHERE admin_id IN( " +
            "SELECT id FROM tb_admin WHERE login_name= #{username}))))) " +
            "UNION " +
            "SELECT * FROM tb_menu WHERE id IN ( " +
            "SELECT parent_id FROM tb_menu WHERE id IN ( " +
            "SELECT parent_id FROM tb_menu WHERE id IN ( " +
            "SELECT menu_id FROM tb_resource_menu  WHERE resource_id IN( " +
            "SELECT resource_id FROM tb_role_resource WHERE role_id IN( " +
            "SELECT role_id FROM tb_admin_role WHERE admin_id IN( " +
            "SELECT id FROM tb_admin WHERE login_name= #{username}))))))")
    public List<Menu> findMenuByUserName(@Param("username") String username);
}
