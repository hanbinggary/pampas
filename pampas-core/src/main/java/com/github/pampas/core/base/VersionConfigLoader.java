/*
 *
 *  *  Copyright 2009-2018.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.github.pampas.core.base;

import com.github.pampas.common.config.ConfigLoader;
import com.github.pampas.common.config.Configurable;
import com.github.pampas.common.config.VersionConfig;
import com.github.pampas.common.extension.SpiMeta;
import com.github.pampas.common.tools.ClassTools;
import com.github.pampas.core.route.RouteRuleConfig;
import com.github.pampas.core.route.rule.HttpRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 * Created by darrenfu on 18-3-8.
 *
 * @author: darrenfu
 * @date: 18 -3-8
 */
@SpiMeta(name = "config-context", order = 50)
public class VersionConfigLoader implements ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(VersionConfigLoader.class);

    private static final Byte ONE = Byte.valueOf("1");

    private static ConcurrentHashMap<Class<? extends VersionConfig>, WeakHashMap<Configurable, Byte>> configAndConfigurableMap = new ConcurrentHashMap<>();

    //VersionConfig 实例缓存    VersionConfig都保持单例
    private static ConcurrentHashMap<Class<? extends VersionConfig>, VersionConfig> configInstanceMap = new ConcurrentHashMap<>();


    /**
     * 缓存VersionConfig和Configurable的关系
     *
     * @param configClz    the config clz
     * @param configurable the configurable
     */
    @Override
    public void markConfigurable(Class<? extends VersionConfig> configClz, Configurable configurable) {

        if (configAndConfigurableMap.contains(configClz)) {
            WeakHashMap<Configurable, Byte> instanceMap = configAndConfigurableMap.get(configClz);
            instanceMap.put(configurable, ONE);
        } else {
            WeakHashMap<Configurable, Byte> instanceMap = new WeakHashMap<>();
            instanceMap.put(configurable, ONE);
            configAndConfigurableMap.putIfAbsent(configClz, instanceMap);
        }
    }


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        Map<String, WeakHashMap<Object, Byte>> map = new HashMap();

        WeakHashMap<Object, Byte> weakHashMap = new WeakHashMap<>();
        VersionConfigLoader c1 = new VersionConfigLoader();
        VersionConfigLoader c2 = new VersionConfigLoader();

        weakHashMap.put(c1, Byte.MIN_VALUE);
        weakHashMap.put(c2, Byte.MIN_VALUE);
        System.out.println("weakHashMap size:" + weakHashMap.size());

        map.put("txt", weakHashMap);
        c2 = null;
        System.gc();

        for (Map.Entry<String, WeakHashMap<Object, Byte>> entry : map.entrySet()) {
            WeakHashMap<Object, Byte> value = entry.getValue();

            System.out.println("weakHashMap size:" + value.size());


        }


    }


    /**
     * Load config t.
     *
     * @param <T>       the type parameter
     * @param configClz the config clz
     * @return the t
     */
    @Override
    public <T extends VersionConfig> T loadConfig(Class<T> configClz) {

        VersionConfig config = configInstanceMap.get(configClz);

        if (config == null) {
            T instance = ClassTools.instance(configClz);
            instance.setupWithDefault();
            ///todo : load config from remote server

            if (configClz == RouteRuleConfig.class) {
                log.info("加载RouteRuleConfig配置");
                instance = (T) buildRouteRuleConfig();
            }

            configInstanceMap.putIfAbsent(configClz, instance);
            config = configInstanceMap.get(configClz);
        }
        return (T) config;
    }

    /**
     * 测试使用
     *
     * @return
     */
    private RouteRuleConfig buildRouteRuleConfig() {
        RouteRuleConfig config = new RouteRuleConfig();
        config.setStripPrefix(true);

        HttpRule httpRule = new HttpRule();
        httpRule.setPath("/admin/**");
        httpRule.setService("admin_service");
        httpRule.setStripPrefix(true);

        config.addRules(httpRule);
        return config;

    }

}
