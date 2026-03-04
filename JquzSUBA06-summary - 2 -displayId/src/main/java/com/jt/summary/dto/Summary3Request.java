package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Summary3Request {

  @JsonProperty("requestBody")
  private RequestBody requestBody;

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RequestBody {

    @JsonProperty("Form")
    private FormDto form;

    @JsonProperty("case")
    private CaseDto caseDto;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FormDto {

    @JsonProperty("F_meisai")
    private List<MeisaiRowDto> meisai;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MeisaiRowDto {

    @JsonProperty("F_summary1")
    private String fSummary1;

    @JsonProperty("F_summary2")
    private String fSummary2;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CaseDto {

    @JsonProperty("caseType")
    private String caseType;

    @JsonProperty("categoryLevel1")
    private CategoryDto categoryLevel1;

    @JsonProperty("categoryLevel2")
    private CategoryDto categoryLevel2;

    @JsonProperty("extensions")
    private ExtensionsDto extensions;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CategoryDto {

    @JsonProperty("displayId")
    private String displayId;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ExtensionsDto {

    @JsonProperty("TransactionDate")
    private String transactionDate;
  }
}
