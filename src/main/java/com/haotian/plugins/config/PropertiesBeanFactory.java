package com.haotian.plugins.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesBeanFactory implements FactoryBean<Properties>, InitializingBean {
    private static final PathMatchingResourcePatternResolver PMRPR = new PathMatchingResourcePatternResolver(PropertiesBeanFactory.class.getClassLoader());
    private final Logger logger =  Logger.getLogger(this.getClass().getName());
    private Properties mergedProp = new Properties();

    private List<String> locations;
    public Properties getObject() throws Exception {
        return mergedProp;
    }

    public Class<?> getObjectType() {
        return Properties.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public void afterPropertiesSet() throws Exception {
        List<Properties> propList = new ArrayList<Properties>();
        for (int i = 0; locations != null && i < locations.size(); i++) {
            loadProps(propList, locations.get(i));
        }
        String propertyFile = System.getProperty("propertyFile");
        if (propertyFile == null || "".equals(propertyFile)) {
			for (Properties prop: propList) {
				CollectionUtils.mergePropertiesIntoMap(prop, mergedProp);
			}
            return;
        }
        if (!propertyFile.startsWith("file:") && !propertyFile.startsWith("classpath:")) {
            propertyFile = "file:" + propertyFile;
            loadProps(propList, propertyFile);
        }
        for (Properties prop: propList) {
            CollectionUtils.mergePropertiesIntoMap(prop, mergedProp);
        }
    }

    private void loadProps(List<Properties> propList, String propertiesFile) throws Exception {
        String classpathPro = "classpath:";
        if (propertiesFile.startsWith(classpathPro)) {
            String formatedFilePath = propertiesFile.replace("/", File.separator).substring(classpathPro.length());
            File tmpFile = new File(PMRPR.getResource(classpathPro + "./").getFile().getCanonicalPath() + File.separator + formatedFilePath);
            formatedFilePath = "file:" + tmpFile.getCanonicalPath();
            logger.info("trans path[" + propertiesFile + "] to path[" + formatedFilePath + "].");
            propertiesFile = formatedFilePath;
        }
        Resource[] resources = PMRPR.getResources(propertiesFile);
        for (Resource resource : resources) {
            if (!resource.exists()) {
                logger.info("can not load config file:[" + propertiesFile + "]. it is not exists.");
                continue;
            }
            logger.info("load config file:[" + propertiesFile + "]");
            propList.add(loadPropFromResource(resource, propertiesFile));
        }
    }

    private Properties loadPropFromResource(Resource resource, String propertiesFile) {
        Properties prop = new Properties();
        InputStream propInput = null;
        try {
            propInput = resource.getInputStream();
            prop.load(propInput);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "load prop config [" + propertiesFile + "] error:" + e.getMessage());
        } finally {
            if (propInput != null) {
                try {
                    propInput.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "close prop config [" + propertiesFile + "] stream error:" + e.getMessage());
                }
            }
        }
        return prop;
    }
}
