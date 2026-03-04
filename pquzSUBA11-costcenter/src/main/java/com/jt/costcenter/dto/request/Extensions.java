package com.jt.costcenter.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Extensions {

    @JsonProperty("CompanyCode")
    private String companyCode;

    @JsonProperty("TransactionDate")
    private String transactionDate;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    // public String pickTransactionDate() {
    //     return transactionDate;
    // }
}