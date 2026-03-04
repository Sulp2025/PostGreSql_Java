package com.jt.costcenter.service;

import java.time.LocalDate;

import com.jt.costcenter.dto.response.CostCenterResponse;

public interface CostCenterService {
  CostCenterResponse getCostCenters(String companyCode, LocalDate transactionDate);
}
