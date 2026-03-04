package com.jt.costcenter.repository.impl;

import com.jt.costcenter.model.CostCenterRow;
import com.jt.costcenter.repository.CostCenterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class CostCenterRepositoryImpl implements CostCenterRepository {

  private final JdbcTemplate jdbcTemplate;

  public CostCenterRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // 使用实际表字段映射
  private static final RowMapper<CostCenterRow> ROW_MAPPER = (rs, rowNum) -> new CostCenterRow(
      rs.getString("costcenter"),
      rs.getString("costcenter_description"));

  @Override

  @SuppressWarnings("null")
  public List<CostCenterRow> findValidCostCenters(String companyCode, LocalDate transactionDate) {

    String sql = """
        SELECT DISTINCT
            costcenter,
            costcenter_description
        FROM public.zquc001
        WHERE companycode = ?
          AND valid_from <= ?
          AND valid_to   >= ?
        ORDER BY costcenter
        """;

    Date sqlDate = Date.valueOf(transactionDate);

    // 参数顺序必须严格对应 SQL 中的 ? 顺序
    return jdbcTemplate.query(sql, ROW_MAPPER, companyCode, sqlDate, sqlDate);
  }
}