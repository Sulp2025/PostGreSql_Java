package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Summary1Request {

  private RequestBody requestBody; 

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RequestBody {

    @JsonProperty("case")
    private CaseDto caseDto; 
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CaseDto {
    private String caseType;                 // JSON: caseType
    private CategoryDto categoryLevel1;      // JSON: categoryLevel1
    private CategoryDto categoryLevel2;      // JSON: categoryLevel2
    private ExtensionsDto extensions;        // JSON: extensions
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
