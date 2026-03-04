package com.jt.costcenter.model;

public class CostCenterRow {
    
    private final String costCenter;
    private final String costCenterDescription;

    public CostCenterRow(String costCenter, String costCenterDescription) {
        this.costCenter = costCenter;
        this.costCenterDescription = costCenterDescription;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public String getCostCenterDescription() {
        return costCenterDescription;
    }
}