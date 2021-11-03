package ru.study21.jcsv.xxl.common;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.util.Map;
import java.util.Set;

public class Configuration {
    private final static Config config = ConfigFactory.load().getConfig("jcsv-xxl");

    public static Config getConfig() {
        return config;
    }

    public static Set<Map.Entry<String, ConfigValue>> getSettings() {
        return config.entrySet();
    }
}
