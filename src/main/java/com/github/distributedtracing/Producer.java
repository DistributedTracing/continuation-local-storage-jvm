package com.github.distributedtracing;

/**
 * A FunctionalInterface, but compatible with
 * earlier Java versions.
 */
public interface Producer<R> {
    R apply();
}
