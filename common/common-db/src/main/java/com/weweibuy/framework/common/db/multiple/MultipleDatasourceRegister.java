package com.weweibuy.framework.common.db.multiple;

import com.weweibuy.framework.common.db.properties.DataSourceWithMybatisProperties;
import com.weweibuy.framework.common.db.properties.MapperScanMybatisProperties;
import com.weweibuy.framework.common.db.properties.MultipleDataSourceProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * @author durenhao
 * @date 2021/7/18 10:32
 **/
public class MultipleDatasourceRegister implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ApplicationContextAware {

    private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private Environment environment;

    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        BindResult<MultipleDataSourceProperties> restServiceBindResult = Binder.get(environment)
                .bind("common.db", MultipleDataSourceProperties.class);
        MultipleDataSourceProperties monitorRestBeanConfig = restServiceBindResult.get();
        Map<String, DataSourceWithMybatisProperties> coreRestServices = monitorRestBeanConfig.getMultipleDatasource();

        coreRestServices.entrySet().stream()
                .peek(e -> registryDatasourceFactoryBean(e.getKey(), e.getValue(), registry))
                .filter(e -> CollectionUtils.isNotEmpty(e.getValue().getMybatis()))
                .forEach(e -> e.getValue().getMybatis().stream()
                        .peek(m -> registrySqlSessionFactoryBean(e.getKey(), m, registry))
                        .forEach(m -> registerMapperScan(e.getKey(), m, registry))
                );

    }

    /**
     * 注册 Datasource
     *
     * @param beanName
     * @param dataSourceProperties
     * @param registry
     */
    private void registryDatasourceFactoryBean(String beanName, DataSourceWithMybatisProperties dataSourceProperties, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(DatasourceFactoryBean.class)
                .addPropertyValue("dataSourceProperties", dataSourceProperties)
                .addPropertyValue("name", beanName)
                .setPrimary(Optional.ofNullable(dataSourceProperties.getPrimary()).orElse(false))
                .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        AbstractBeanDefinition definition = definitionBuilder.getBeanDefinition();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }


    /**
     * 注册 SqlSessionFactory
     *
     * @param datasourceName
     * @param mybatisProperties
     * @param registry
     */
    public void registrySqlSessionFactoryBean(String datasourceName, MapperScanMybatisProperties mybatisProperties, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(CustomSqlSessionFactoryBean.class)
                .addPropertyValue("datasourceName", datasourceName)
                .addPropertyValue("properties", mybatisProperties)
                .setPrimary(Optional.ofNullable(mybatisProperties.getPrimary()).orElse(false))
                .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        AbstractBeanDefinition definition = definitionBuilder.getBeanDefinition();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, sqlSessionFactoryBeanName(datasourceName));
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }


    /**
     * 注册 mapperScan
     *
     * @param datasourceName
     * @param properties
     * @param registry
     */
    public void registerMapperScan(String datasourceName, MapperScanMybatisProperties properties, BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
        builder.addPropertyValue("processPropertyPlaceHolders", true);

        builder.addPropertyValue("sqlSessionFactoryBeanName", sqlSessionFactoryBeanName(datasourceName));

        builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(properties.getMapperScanPackages()));

        registry.registerBeanDefinition(datasourceName + "MapperScannerConfigurer", builder.getBeanDefinition());

    }

    private String sqlSessionFactoryBeanName(String datasourceName) {
        return datasourceName + "SqlSessionFactory";
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
