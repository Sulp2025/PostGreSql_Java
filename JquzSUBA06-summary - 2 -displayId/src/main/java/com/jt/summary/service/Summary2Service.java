package com.jt.summary.service;

import com.jt.summary.dto.Summary2Request;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.dto.SummaryResponse.CodeDto;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.impl.Category2RuleService; // MOD: yml rule service (adjust if package differs)
import com.jt.summary.util.DateParsers;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class Summary2Service {

  private static final String CODE_S000 = "S000";
  private static final String TYPE_INFO = "INFO";
  private static final String MSG_SUCCESS = "Success";

  private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD: inject yml-driven rule service

  public SummaryResponse getSummary2(Summary2Request req) {

    try {
      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
      }

      var c = req.getRequestBody().getCaseDto();

      String caseType = trim(c.getCaseType());

      // MOD: category1/category2 use displayId (already)
      String category1DisplayId =
          trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId =
          trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD

      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      String summary1 = trim(extractSummary1FromForm(req));
      String summary1Param = isBlank(summary1) ? "" : summary1;

      if (isBlank(caseType)) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: caseType is required");
      }
      if (isBlank(category1DisplayId)) { // MOD
        // MOD: message still says name, but value is displayId now
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: categoryLevel1.displayId is required"); // MOD
      }
      if (isBlank(txStr)) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: extensions.TransactionDate is required");
      }

      // MOD: use yml rules (caseType + category1_displayid)
      boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId); // MOD

      if (needCategory2 && isBlank(category2DisplayId)) { // MOD
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
      }

      LocalDate txDate = DateParsers.parseFlexible(txStr);

      // MOD: if not needed, pass "" to repo
      String category2Param = needCategory2 ? category2DisplayId : ""; // MOD

      List<String> summary2List = repo.findDistinctSummary2(
          caseType,
          category1DisplayId,   // MOD
          category2Param,       // MOD
          summary1Param,
          txDate,
          needCategory2);

      if (summary2List == null) {
        summary2List = Collections.emptyList();
      }

      List<CodeDto> codes = summary2List.stream()
          .filter(s -> s != null && !s.isBlank())
          .distinct()
          .map(s -> new CodeDto(s, ""))
          .collect(Collectors.toList());

      // 7) 成功固定（0件でも Success のまま）
      return buildSuccess(codes);

    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Internal Server Error: " + safeExString(ex),
          ex);
    }
  }

  private static SummaryResponse buildSuccess(List<CodeDto> codes) {
    return new SummaryResponse(
        new SummaryResponse.ResponseBody(
            List.of(new SummaryResponse.MessageDto(CODE_S000, MSG_SUCCESS, TYPE_INFO)),
            new SummaryResponse.ValueDto(codes == null ? Collections.emptyList() : codes),
            true));
  }

  private static String safeExString(Exception ex) {
    if (ex == null)
      return "";
    String msg = ex.getMessage();
    String name = ex.getClass().getSimpleName();
    return (msg == null || msg.trim().isEmpty()) ? name : (name + ": " + msg);
  }

  private String extractSummary1FromForm(Summary2Request req) {
    var rb = req.getRequestBody();
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return null;
    }

    for (var row : rb.getForm().getMeisai()) {
      if (row != null && !isBlank(row.getFSummary1())) {
        return row.getFSummary1();
      }
    }
    return null;
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private String trim(String s) {
    return s == null ? null : s.trim();
  }
}

// package com.jt.summary.service;

// import com.jt.summary.dto.Summary2Request;
// import com.jt.summary.dto.SummaryResponse;
// import com.jt.summary.dto.SummaryResponse.CodeDto;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.util.DateParsers;
// import java.time.LocalDate;
// import java.util.Collections;
// import java.util.List;
// import java.util.Set;
// import java.util.stream.Collectors;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.web.server.ResponseStatusException;

// @Service
// @RequiredArgsConstructor
// public class Summary2Service {

//   /**
//    * 固定資産（Fixed Asset）系の caseType。
//    */
//   private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//       "ZD11");

//   // Category Level1
//   @SuppressWarnings("unused")
//   private static final String CATEGORY1_DOMESTIC = "国内";
//   private static final String CATEGORY1_FOREIGN = "海外";
//   private static final String CATEGORY1_OVERSEAS = "Overseas";
//   private static final String CATEGORY1_INCOME = "収入金計上";

//   /**
//    * Category2 を不要（無視）とする Category1 一覧
//    * ※ただし「固定資産 caseType」の場合は、この一覧に該当しても Category2 必須
//    */
//   private static final Set<String> CATEGORY1_IGNORE_CATEGORY2 = Set.of(
//       CATEGORY1_FOREIGN,
//       CATEGORY1_OVERSEAS,
//       CATEGORY1_INCOME);

//   private static final String CODE_S000 = "S000";
//   private static final String TYPE_INFO = "INFO";
//   private static final String MSG_SUCCESS = "Success";

//   private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

//   private final TblSumRepository repo;

//   public SummaryResponse getSummary2(Summary2Request req) {

//     try {
//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         throw new ResponseStatusException(
//             HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
//       }

//       var c = req.getRequestBody().getCaseDto();

//       String caseType = trim(c.getCaseType());
//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());
//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       String summary1 = trim(extractSummary1FromForm(req));
//       String summary1Param = isBlank(summary1) ? "" : summary1;

//       if (isBlank(caseType)) {
//         throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//             "Internal Server Error: caseType is required");
//       }
//       if (isBlank(category1)) {
//         throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//             "Internal Server Error: categoryLevel1.name is required");
//       }
//       if (isBlank(txStr)) {
//         throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//             "Internal Server Error: extensions.TransactionDate is required");
//       }

//       boolean needCategory2 = isCategory2Required(caseType, category1);

//       if (needCategory2 && isBlank(category2)) {
//         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
//       }

//       LocalDate txDate = DateParsers.parseFlexible(txStr);

//       String category2Param = needCategory2 ? category2 : "";

//       List<String> summary2List = repo.findDistinctSummary2(
//           caseType,
//           category1,
//           category2Param,
//           summary1Param,
//           txDate,
//           needCategory2);

//       if (summary2List == null) {
//         summary2List = Collections.emptyList();
//       }

//       List<CodeDto> codes = summary2List.stream()
//           .filter(s -> s != null && !s.isBlank())
//           .distinct()
//           .map(s -> new CodeDto(s, ""))
//           .collect(Collectors.toList());

//       // 7) 成功固定（0件でも Success のまま）
//       return buildSuccess(codes);

//     } catch (ResponseStatusException ex) {
//       throw ex;
//     } catch (Exception ex) {
//       throw new ResponseStatusException(
//           HttpStatus.INTERNAL_SERVER_ERROR,
//           "Internal Server Error: " + safeExString(ex),
//           ex);
//     }
//   }

//   /**
//    * Category2必須判定
//    * - 固定資産 caseType: 常に必須
//    * - それ以外：Category1 が IGNORE 一覧なら不要、そうでなければ必須
//    */
//   private boolean isCategory2Required(String caseType, String category1) {
//     // 固定資産：国内だけでなく海外でも Category2 必須
//     if (isFixedAssetCase(caseType)) {
//       return true;
//     }

//     String c1 = trim(category1);
//     if (c1 == null || c1.isBlank()) {
//       // 安全側
//       return true;
//     }

//     // 海外/Overseas/収入金計上 => no Category2
//     if (CATEGORY1_IGNORE_CATEGORY2.contains(c1)) {
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
//             new SummaryResponse.ValueDto(codes == null ? Collections.emptyList() : codes),
//             true));
//   }

//   private static String safeExString(Exception ex) {
//     if (ex == null)
//       return "";
//     String msg = ex.getMessage();
//     String name = ex.getClass().getSimpleName();
//     return (msg == null || msg.trim().isEmpty()) ? name : (name + ": " + msg);
//   }

//   private String extractSummary1FromForm(Summary2Request req) {
//     var rb = req.getRequestBody();
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       return null;
//     }

//     for (var row : rb.getForm().getMeisai()) {
//       if (row != null && !isBlank(row.getFSummary1())) {
//         return row.getFSummary1();
//       }
//     }
//     return null;
//   }

//   private boolean isBlank(String s) {
//     return s == null || s.trim().isEmpty();
//   }

//   private String trim(String s) {
//     return s == null ? null : s.trim();
//   }
// }
