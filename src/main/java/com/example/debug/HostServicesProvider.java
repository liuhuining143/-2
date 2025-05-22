package com.example.debug;

import javafx.application.HostServices;

public class HostServicesProvider {
    private static HostServices hostServices;

    public static void init(HostServices services) {
        hostServices = services;
    }

    public static HostServices getHostServices() {
        if (hostServices == null) {
            throw new IllegalStateException("HostServices未初始化，请在Application.start()中调用init()");
        }
        return hostServices;
    }
}