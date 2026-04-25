package com.tianji.learning.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableInfoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.transform.sax.TemplatesHandler;
import java.util.HashMap;
import java.util.Map;

@Configuration
// 配置Mybatis的（动态修改表名）类，注入到本项目容器中
public class MybatisConfiguration {

    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor() {
        // map 存储原始表名和 新的表名
        Map<String, TableNameHandler> map = new HashMap<>(1);
        map.put("points_board", (sql, tableName) -> TableInfoContext.getInfo());
        return new DynamicTableNameInnerInterceptor(map);
    }



}
