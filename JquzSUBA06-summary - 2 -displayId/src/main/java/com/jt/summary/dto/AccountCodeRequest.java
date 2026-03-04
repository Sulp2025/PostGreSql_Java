
package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
//import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
//import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class AccountCodeRequest {

  @JsonProperty("requestBody")
  @JsonAlias({ "RequestBody", "request_body" })
  private RequestBody requestBody;

  @Data
  public static class RequestBody {

    @JsonProperty("case")
    @JsonAlias({ "caseDto", "Case" })
    private CaseDto caseDto;

    @JsonProperty("Form")
    @JsonAlias({ "form", "FORM" })
    private FormDto form;
  }

  @Data
  public static class CaseDto {

    @JsonProperty("caseType")
    @JsonAlias({ "CaseType", "CASE_TYPE" })
    private String caseType;

    @JsonProperty("categoryLevel1")
    @JsonAlias({ "CategoryLevel1", "category1" })
    private CategoryLevel1 categoryLevel1;

    @JsonProperty("categoryLevel2")
    @JsonAlias({ "CategoryLevel2", "category2" })
    private CategoryLevel2 categoryLevel2;

    @JsonProperty("extensions")
    @JsonAlias({ "Extensions", "extension" })
    private Extensions extensions;
  }

  @Data
  public static class CategoryLevel1 {
    // @JsonProperty("name")
    // @JsonAlias({ "Name" })
    // private String name;
    @JsonProperty("displayId")
    @JsonAlias({
        "DisplayId", "displayID",
        "category1_displayid", "category1DisplayId", "category1_displayId"
    })
    private String displayId;
  }

  @Data
  public static class CategoryLevel2 {
    // @JsonProperty("name")
    // @JsonAlias({ "Name" })
    // private String name;
    @JsonProperty("displayId")
    @JsonAlias({
        "DisplayId", "displayID",
        "category2_displayid", "category2DisplayId", "category2_displayId"
    })
    private String displayId;
  }

  @Data
  public static class Extensions {
    /**
     * TransactionDate comes from UI/integration and may be formatted in multiple
     * ways
     * (yyyy-MM-dd / yyyy/MM/dd / yyyy/M/d / timestamp-like strings).
     *
     * We accept it as String here and parse it in
     * {@link com.jt.summary.util.RequestContext}
     * via {@link com.jt.summary.util.DateParsers#parseFlexible(String)}.
     */
    @JsonProperty("TransactionDate")
    @JsonAlias({ "transactionDate", "TRANSACTION_DATE" })
    private String transactionDate;
  }

  @Data
  public static class FormDto {

    @JsonProperty("F_Settlement_1")
    @JsonAlias({ "F_Settlement1", "F_Settlement_01", "F_settlement1" })
    private String fSettlement1;

    @JsonProperty("F_Settlement_2")
    @JsonAlias({ "F_Settlement2", "F_Settlement_02", "F_settlement2" })
    private String fSettlement2;

    @JsonProperty("F_Settlement_3")
    @JsonAlias({ "F_Settlement3", "F_Settlement_03", "F_settlement3" })
    private String fSettlement3;

    @JsonProperty("F_Invoice_Number")
    @JsonAlias({ "F_InvoiceNumber", "F_Invoice_number" })
    private String fInvoiceNumber;

    @JsonProperty("F_Total_amount_c")
    @JsonAlias({ "F_Total_amount_C", "F_total_amount_c" })
    private String fTotalAmountC;

    @JsonProperty("F_Rate")
    @JsonAlias({ "F_rate", "Rate", "rate", "F_Rate_1" })
    private BigDecimal fRate;

    @JsonProperty("F_meisai")
    @JsonAlias({ "F_Meisai", "meisai" })
    private List<MeisaiRowDto> meisai;
  }

  @Data
  public static class MeisaiRowDto {

    @JsonProperty("F_Quantity")
    @JsonAlias({ "F_Quantity_1", "F_quantity" })
    private String fQuantity1; //

    @JsonProperty("F_Doc_curr_amt")
    @JsonAlias({ "F_Doc_curr_Amt", "F_doc_curr_amt" })
    private String fDocCurrAmt;

    @JsonProperty("F_Dome_curr_amt")
    @JsonAlias({ "F_Dome_curr_Amt", "F_dome_curr_amt" })
    private String fDomeCurrAmt;

    @JsonProperty("F_summary1")
    @JsonAlias({ "F_Summary1", "F_SUMMARY1" })
    private String fSummary1;

    @JsonProperty("F_summary2")
    @JsonAlias({ "F_Summary2", "F_SUMMARY2" })
    private String fSummary2;

    @JsonProperty("F_summary3")
    @JsonAlias({ "F_Summary3", "F_SUMMARY3" })
    private String fSummary3;

    @JsonProperty("F_summary4")
    @JsonAlias({ "F_Summary4", "F_SUMMARY4" })
    private String fSummary4;

    @JsonProperty("F_summary5")
    @JsonAlias({ "F_Summary5", "F_SUMMARY5" })
    private String fSummary5;

    @JsonProperty("F_summary6")
    @JsonAlias({ "F_Summary6", "F_SUMMARY6" })
    private String fSummary6;

    @JsonProperty("F_Account_Code")
    @JsonAlias({ "F_Account_code", "FAccountCode" })
    private String fAccountCode;

    @JsonProperty("F_Enter_expenses")
    @JsonAlias({ "F_Enter_Expenses", "FEnterExpenses" })
    private String fEnterExpenses;

    @JsonProperty("F_Tax_amount")
    @JsonAlias({ "F_Tax_Amount", "F_tax_amount", "F_TaxAmount", "TaxAmount" })
    private String fTaxAmount;

    @JsonProperty("F_Tax_code")
    @JsonAlias({ "F_Tax_Code", "F_tax_code", "TaxCode" })
    private String fTaxCode;
  }
}
