package com.ly.common.domain;

import java.util.Map;

public class Conclusion {
    public static final int FINISH_OK=0;
    public static final int ERROR=-1;
    public static final int STEP_SUCCESS=1;
    public static final int FAILED=2;
    private int finalConclusion=-1;
    private int tempConclusion=-1;
    private Map<String,Integer> historyConclusions;
    public Conclusion(int finalConclusion){
        this.finalConclusion=finalConclusion;
    }
    public Conclusion(int finalConclusion,int tempConclusion){
        this.finalConclusion=finalConclusion;
        this.tempConclusion=tempConclusion;
    }
    public int getFinalConclusion() {
        return finalConclusion;
    }

    public void setFinalConclusion(int finalConclusion) {
        this.finalConclusion = finalConclusion;
    }

    public int getTempConclusion() {
        return tempConclusion;
    }

    public void setTempConclusion(int tempConclusion) {
        this.tempConclusion = tempConclusion;
    }

    public Map<String, Integer> getHistoryConclusions() {
        return historyConclusions;
    }

    public void setHistoryConclusions(Map<String, Integer> historyConclusions) {
        this.historyConclusions = historyConclusions;
    }
}
