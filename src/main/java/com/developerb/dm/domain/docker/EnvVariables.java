package com.developerb.dm.domain.docker;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * For defining environment variables
 */
public class EnvVariables {

    private final Map<String, String> variables = Maps.newHashMap();

    public EnvVariables add(String formatted) {
        String[] split = formatted.split("=");
        return add(split[0], split[1]);
    }

    public EnvVariables add(String key, String value) {
        variables.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnvVariables that = (EnvVariables) o;
        return variables.equals(that.variables);
    }

    @Override
    public int hashCode() {
        return variables.hashCode();
    }

    @Override
    public String toString() {
        return Joiner.on(", ")
                .withKeyValueSeparator("=")
                .join(variables);
    }

    public String[] toEnv() {
        String[] env = new String[variables.size()];

        int counter = 0;
        for (String key : variables.keySet()) {
            String value = variables.get(key);
            env[counter++] = key + "=" + value;
        }

        return env;
    }

}
