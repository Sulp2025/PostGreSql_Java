package com.jt.summary.service.impl;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.repository.TblSumRepository;
//import com.jt.summary.service.impl.Category2RuleService
import com.jt.summary.service.SummaryDropdownService;
import com.jt.summary.util.RequestContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryDropdownServiceImpl implements SummaryDropdownService {

  private final TblSumRepository repo;
  private final Category2RuleService ruleService; // MOD

  @Override
  public SummaryResponse getSummaryOptions(int level, AccountCodeRequest req) {

    RequestContext ctx = RequestContext.from(req);

    if (ctx.isMeisaiEmpty()) {
      return SummaryResponse.success(List.of());
    }

    // MOD: now category1/category2 are displayId
    String caseType = trimToEmpty(ctx.getCaseType());
    String category1DisplayId = trimToEmpty(ctx.getCategory1()); // MOD
    String category2DisplayId = trimToEmpty(ctx.getCategory2()); // MOD

    // MOD: yml driven decision
    boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId);

    String category2Param = needCategory2 ? category2DisplayId : "";

    String s1 = trimToNull(ctx.getSummary(1));
    String s2 = trimToNull(ctx.getSummary(2));
    String s3 = trimToNull(ctx.getSummary(3));
    String s4 = trimToNull(ctx.getSummary(4));
    String s5 = trimToNull(ctx.getSummary(5));

    String s1Param = (s1 == null ? "" : s1);
    String s2Param = (s2 == null ? "" : s2);

    List<String> values = switch (level) {
      case 1 -> repo.findDistinctSummary1(
          caseType, category1DisplayId, category2Param, ctx.getTransactionDate(), needCategory2); // MOD
      case 2 -> repo.findDistinctSummary2(
          caseType, category1DisplayId, category2Param, s1Param, ctx.getTransactionDate(), needCategory2); // MOD
      case 3 -> repo.findDistinctSummary3(
          caseType, category1DisplayId, category2Param, s1Param, s2Param, ctx.getTransactionDate(), needCategory2); // MOD
      case 4 -> repo.findDistinctSummary4Optional(
          caseType, category1DisplayId, category2Param, s1, s2, s3, ctx.getTransactionDate(), needCategory2); // MOD
      case 5 -> repo.findDistinctSummary5Optional(
          caseType, category1DisplayId, category2Param, s1, s2, s3, s4, ctx.getTransactionDate(), needCategory2); // MOD
      case 6 -> repo.findDistinctSummary6Optional(
          caseType, category1DisplayId, category2Param, s1, s2, s3, s4, s5, ctx.getTransactionDate(), needCategory2); // MOD
      default -> throw new IllegalArgumentException("level must be 1..6");
    };

    var codes = (values == null ? List.<String>of() : values)
        .stream()
        .filter(v -> v != null && !v.isBlank())
        .distinct()
        .map(v -> new SummaryResponse.CodeDto(v, ""))
        .toList();

    return SummaryResponse.success(codes);
  }

  private static String trimToNull(String s) {
    if (s == null)
      return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String trimToEmpty(String s) {
    return s == null ? "" : s.trim();
  }
}