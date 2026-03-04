package com.jt.costcenter.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CostCenterRequest {

  @JsonProperty("requestBody")
  private RequestBody requestBody;

  public RequestBody getRequestBody() {
    return requestBody;
  }

  public void setRequestBody(RequestBody requestBody) {
    this.requestBody = requestBody;
  }

}
