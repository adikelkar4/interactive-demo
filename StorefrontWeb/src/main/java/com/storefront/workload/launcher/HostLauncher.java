package com.storefront.workload.launcher;

public interface HostLauncher {
    void scaleHosts(int count) throws Exception;
}
