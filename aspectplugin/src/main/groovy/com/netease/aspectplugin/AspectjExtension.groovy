package com.netease.aspectplugin

public class AspectjExtension {

    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()

    public AspectjExtension includeJarFilter(String...filters) {
        if (filters != null) {
            includeJarFilter.addAll(filters)
        }

        return this
    }

    public AspectjExtension excludeJarFilter(String...filters) {
        if (filters != null) {
            excludeJarFilter.addAll(filters)
        }

        return this
    }
}