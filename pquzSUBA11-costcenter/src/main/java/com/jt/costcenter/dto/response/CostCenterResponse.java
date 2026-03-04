package com.jt.costcenter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CostCenterResponse {

  @JsonProperty("responseBody")
  private ResponseBody responseBody;

  public CostCenterResponse() {}

  public CostCenterResponse(ResponseBody responseBody) {
    this.responseBody = responseBody;
  }

  public ResponseBody getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(ResponseBody responseBody) {
    this.responseBody = responseBody;
  }
}