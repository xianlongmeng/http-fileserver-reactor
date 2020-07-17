package com.ly.rhdfs.store.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StoreInitialAction implements ApplicationRunner {

    private StoreManager storeManager;

    @Autowired
    public void setStoreManager(StoreManager storeManager) {
        this.storeManager = storeManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        storeManager.initial();
    }
}
