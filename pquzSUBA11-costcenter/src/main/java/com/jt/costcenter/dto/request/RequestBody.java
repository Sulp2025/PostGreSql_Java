package com.jt.costcenter.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestBody {

  @JsonProperty("case")
  private CaseData caseData;

  public CaseData getCaseData() {
    return caseData;
  }

  public void setCaseData(CaseData caseData) {
    this.caseData = caseData;
  }
}
