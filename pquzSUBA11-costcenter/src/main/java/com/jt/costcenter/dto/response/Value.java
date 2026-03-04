package com.jt.costcenter.dto.response;

import java.util.List;

public class Value {

  private List<CodeItem> codes;

  public Value() {}

  public Value(List<CodeItem> codes) {
    this.codes = codes;
  }

  public List<CodeItem> getCodes() {
    return codes;
  }

  public void setCodes(List<CodeItem> codes) {
    this.codes = codes;
  }
}