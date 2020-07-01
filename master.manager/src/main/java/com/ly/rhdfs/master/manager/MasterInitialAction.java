package com.ly.rhdfs.master.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MasterInitialAction implements ApplicationRunner {

    private MasterManager masterManager;

    @Autowired
    public void setMasterManager(MasterManager masterManager) {
        this.masterManager = masterManager;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        masterManager.initial();
    }
}
