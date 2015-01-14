package com.developerb.dm.dsl.hook;

import groovy.lang.Closure;


abstract class AbstractHook<T> {

    private final Closure<?> implementation;

    public AbstractHook(Closure<?> implementation) {
        this.implementation = implementation;
    }

    public final void execute(T arg) {
        if (implementation != null) {
            execute(implementation, arg);
        }
    }

    protected abstract void execute(Closure<?> implementation, T arg);

}
