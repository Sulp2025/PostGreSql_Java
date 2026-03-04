package com.jt.summary.service;

import com.jt.summary.dto.Summary1Request;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.dto.SummaryResponse.CodeDto;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.impl.Category2RuleService;
import com.jt.summary.util.DateParsers;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Summary1Service {

  private static final String MSG_MISSING_REQUIRED =
      "Missing required fields: caseType / category / TransactionDate";
  private static final String MSG_NO_MATCHED =
      "No matched SUMMARY1. Please check Category / TransactionDate";
  private static final String MSG_SELECT_CATEGORY2 =
      "Please select Category Level2.";
  private static final String MSG_SUCCESS = "Success";

  private static final String CODE_E400 = "E400";
  private static final String CODE_E404 = "E404";
  private static final String CODE_E500 = "E500";
  private static final String CODE_S000 = "S000";

  private static final String TYPE_INFO = "INFO";
  private static final String TYPE_ERROR = "ERROR";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD: yml rule service

  public SummaryResponse getSummary1(Summary1Request req) {
    try {
      // 1) Basic null checks
      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        return buildError(CODE_E400, TYPE_INFO, MSG_MISSING_REQUIRED);
      }

      // 2) Unpack + trim
      var c = req.getRequestBody().getCaseDto();
      String caseType = trim(c.getCaseType());

      // MOD: use displayId (not name). This already matches your DB columns and new rules.
      String category1DisplayId =
          trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId =
          trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD

      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      // 3) Mandatory fields
      if (isBlank(caseType) || isBlank(category1DisplayId) || isBlank(txStr)) {
        return buildError(CODE_E400, TYPE_INFO, MSG_MISSING_REQUIRED);
      }

      // 4) Category2 requirement rule (from application.yml)
      boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId); // MOD
      if (needCategory2 && isBlank(category2DisplayId)) {
        return buildError(CODE_E400, TYPE_INFO, MSG_SELECT_CATEGORY2);
      }

      // 5) Parse TransactionDate
      final LocalDate txDate;
      try {
        txDate = DateParsers.parseFlexible(txStr);
        if (txDate == null) {
          return buildError(CODE_E400, TYPE_INFO, "Invalid TransactionDate format: " + txStr + ".");
        }
      } catch (IllegalArgumentException ex) {
        return buildError(CODE_E400, TYPE_INFO, "Invalid TransactionDate format: " + txStr + ".");
      }

      // 6) Query DB (keep repo signature unchanged)
      String category2Param = needCategory2 ? category2DisplayId : ""; // MOD
      List<String> summary1List =
          repo.findDistinctSummary1(caseType, category1DisplayId, category2Param, txDate, needCategory2); // MOD

      // 7) No matched
      if (summary1List == null || summary1List.isEmpty()) {
        return buildError(CODE_E404, TYPE_INFO, MSG_NO_MATCHED);
      }

      // 8) Build response codes
      List<CodeDto> codes = summary1List.stream()
          .filter(s -> s != null && !s.isBlank())
          .distinct()
          .map(s -> new CodeDto(s, ""))
          .collect(Collectors.toList());

      return buildSuccess(codes);

    } catch (Exception ex) {
      return buildError(CODE_E500, TYPE_ERROR, "Internal Server Error: " + safeExString(ex));
    }
  }

  private static SummaryResponse buildSuccess(List<CodeDto> codes) {
    return new SummaryResponse(
        new SummaryResponse.ResponseBody(
            List.of(new SummaryResponse.MessageDto(CODE_S000, MSG_SUCCESS, TYPE_INFO)),
            new SummaryResponse.ValueDto(codes == null ? List.of() : codes),
            true));
  }

  private static SummaryResponse buildError(String code, String type, String message) {
    return new SummaryResponse(
        new SummaryResponse.ResponseBody(
            List.of(new SummaryResponse.MessageDto(code, message, type)),
            new SummaryResponse.ValueDto(List.of()),
            false));
  }

  private static String safeExString(Exception ex) {
    if (ex == null) return "";
    String msg = ex.getMessage();
    String name = ex.getClass().getSimpleName();
    return (msg == null || msg.trim().isEmpty()) ? name : (name + ": " + msg);
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private String trim(String s) {
    return s == null ? null : s.trim();
  }
}


// package com.jt.summary.service;

// import com.jt.summary.dto.Summary1Request;
// import com.jt.summary.dto.SummaryResponse;
// import com.jt.summary.dto.SummaryResponse.CodeDto;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.util.DateParsers;
// import java.time.LocalDate;
// import java.util.List;
// import java.util.Set;
// import java.util.stream.Collectors;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor
// public class Summary1Service {

//   /**
//    * 固定資産（Fixed Asset）系の caseType。
//    */
//   private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//       "ZD11");

//   // Category Level1
//   private static final String CATEGORY1_DOMESTIC = "国内";
//   private static final String CATEGORY1_FOREIGN = "海外";
//   private static final String CATEGORY1_OVERSEAS = "Overseas";

//   // Income-related Category Level1
//   private static final String CATEGORY1_INCOME = "収入金計上";

//   private static final String MSG_MISSING_REQUIRED = "Missing required fields: caseType / category / TransactionDate";
//   private static final String MSG_NO_MATCHED = "No matched SUMMARY1. Please check Category / TransactionDate";
//   private static final String MSG_SELECT_CATEGORY2 = "Please select Category Level2.";
//   private static final String MSG_SUCCESS = "Success";

//   private static final String CODE_E400 = "E400";
//   private static final String CODE_E404 = "E404";
//   private static final String CODE_E500 = "E500";
//   private static final String CODE_S000 = "S000";

//   private static final String TYPE_INFO = "INFO";
//   private static final String TYPE_ERROR = "ERROR";

//   private final TblSumRepository repo;

//   public SummaryResponse getSummary1(Summary1Request req) {
//     try {
//       // 1) Basic null checks
//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         return buildError(CODE_E400, TYPE_INFO, MSG_MISSING_REQUIRED);
//       }

//       // 2) Unpack + trim
//       var c = req.getRequestBody().getCaseDto();
//       String caseType = trim(c.getCaseType());

//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());

//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       // 3) Mandatory fields
//       if (isBlank(caseType) || isBlank(category1) || isBlank(txStr)) {
//         return buildError(CODE_E400, TYPE_INFO, MSG_MISSING_REQUIRED);
//       }

//       // 4) Category2 requirement rule
//       boolean needCategory2 = isCategory2Required(caseType, category1);
//       if (needCategory2 && isBlank(category2)) {
//         return buildError(CODE_E400, TYPE_INFO, MSG_SELECT_CATEGORY2);
//       }

//       // 5) Parse TransactionDate
//       final LocalDate txDate;
//       try {
//         txDate = DateParsers.parseFlexible(txStr);
//         if (txDate == null) {
//           return buildError(CODE_E400, TYPE_INFO, "Invalid TransactionDate format: " + txStr + ".");
//         }
//       } catch (IllegalArgumentException ex) {
//         return buildError(CODE_E400, TYPE_INFO, "Invalid TransactionDate format: " + txStr + ".");
//       }

//       // 6) Query DB (keep repo signature unchanged)
//       String category2Param = needCategory2 ? category2 : "";
//       List<String> summary1List = repo.findDistinctSummary1(caseType, category1, category2Param, txDate, needCategory2);

//       // 7) No matched
//       if (summary1List == null || summary1List.isEmpty()) {
//         return buildError(CODE_E404, TYPE_INFO, MSG_NO_MATCHED);
//       }

//       // 8) Build response codes
//       List<CodeDto> codes = summary1List.stream()
//           .filter(s -> s != null && !s.isBlank())
//           .distinct()
//           .map(s -> new CodeDto(s, ""))
//           .collect(Collectors.toList());

//       return buildSuccess(codes);

//     } catch (Exception ex) {
//       return buildError(CODE_E500, TYPE_ERROR, "Internal Server Error: " + safeExString(ex));
//     }
//   }

//   // 国内 => need Category2
//   // 海外/Overseas/収入金計上 => no Category2
//   // 固定資産 => 国内/海外ともに need Category2
//   private boolean isCategory2Required(String caseType, String category1) {
//     String c1 = trim(category1);

//     // 固定資産：国内だけでなく海外でも Category2 必須
//     if (isFixedAssetCase(caseType)) {
//       return true;
//     }

//     // null/空白：安全側（Category2 必須）
//     if (c1 == null || c1.isBlank()) {
//       return true;
//     }

//     // 国内 => need Category2
//     if (c1.startsWith(CATEGORY1_DOMESTIC)) {
//       return true;
//     }

//     // 海外/Overseas/収入金計上 => no Category2
//     if (c1.startsWith(CATEGORY1_FOREIGN)
//         || CATEGORY1_OVERSEAS.equals(c1)
//         || CATEGORY1_INCOME.equals(c1)) {
//       return false;
//     }

//     // default: need Category2
//     return true;
//   }

//   /** 固定資産かどうか（caseType がコードの場合/名称の場合どちらにも対応） */
//   private boolean isFixedAssetCase(String caseType) {
//     String ct = trim(caseType);
//     if (ct == null || ct.isBlank())
//       return false;

//     // caseType が名称で来る場合
//     if (ct.contains("固定資産") || ct.contains("固定资产")) {
//       return true;
//     }

//     // caseType がコードで来る場合
//     return CASETYPE_ASSET_CATE.contains(ct);
//   }

//   private static SummaryResponse buildSuccess(List<CodeDto> codes) {
//     return new SummaryResponse(
//         new SummaryResponse.ResponseBody(
//             List.of(new SummaryResponse.MessageDto(CODE_S000, MSG_SUCCESS, TYPE_INFO)),
//             new SummaryResponse.ValueDto(codes == null ? List.of() : codes),
//             true));
//   }

//   private static SummaryResponse buildError(String code, String type, String message) {
//     return new SummaryResponse(
//         new SummaryResponse.ResponseBody(
//             List.of(new SummaryResponse.MessageDto(code, message, type)),
//             new SummaryResponse.ValueDto(List.of()),
//             false));
//   }

//   private static String safeExString(Exception ex) {
//     if (ex == null)
//       return "";
//     String msg = ex.getMessage();
//     String name = ex.getClass().getSimpleName();
//     return (msg == null || msg.trim().isEmpty()) ? name : (name + ": " + msg);
//   }

//   private boolean isBlank(String s) {
//     return s == null || s.trim().isEmpty();
//   }

//   private String trim(String s) {
//     return s == null ? null : s.trim();
//   }
// }
