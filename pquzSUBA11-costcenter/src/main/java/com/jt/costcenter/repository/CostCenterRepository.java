package com.jt.costcenter.repository;

import java.time.LocalDate;
import java.util.List;

import com.jt.costcenter.model.CostCenterRow;

public interface CostCenterRepository {
  List<CostCenterRow> findValidCostCenters(String companyCode, LocalDate transactionDate);
}

