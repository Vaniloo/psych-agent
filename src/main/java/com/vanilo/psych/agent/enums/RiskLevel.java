package com.vanilo.psych.agent.enums;

public enum RiskLevel {
    HIGH,MEDIUM,LOW;
    public static RiskLevel fromString(String riskLevel){
        try{
            return RiskLevel.valueOf(riskLevel.toUpperCase());
        }
        catch(RuntimeException e){
            throw new RuntimeException("非法的 Risk Level");
        }
    }
}
