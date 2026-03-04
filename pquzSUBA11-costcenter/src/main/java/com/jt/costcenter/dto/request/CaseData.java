package com.jt.costcenter.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseData {

  @JsonProperty("extensions")
  private Extensions extensions;

  public Extensions getExtensions() {
    return extensions;
  }

  public void setExtensions(Extensions extensions) {
    this.extensions = extensions;
  }
}