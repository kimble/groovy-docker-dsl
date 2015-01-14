package com.developerb.dm.dsl;

import groovy.lang.Script;

/**
 * Stupid class to enable "method missing" in Java classes
 * used as delegates to Groovy closures.
 */
abstract class AbstractDelegate extends Script {

    @Override
    public Object run() {
        return null;
    }

}
