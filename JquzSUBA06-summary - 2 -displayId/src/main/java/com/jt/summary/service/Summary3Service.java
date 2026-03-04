package com.jt.summary.service;

import com.jt.summary.dto.Summary3Request;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.dto.SummaryResponse.CodeDto;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.impl.Category2RuleService; // MOD
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
public class Summary3Service {

  private static final String CODE_S000 = "S000";
  private static final String TYPE_INFO = "INFO";
  private static final String MSG_SUCCESS = "Success";

  private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD

  public SummaryResponse getSummary3(Summary3Request req) {

    try {
      // 0) NPE 防御（仕様：想定外は HTTP 500）
      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
      }

      var rb = req.getRequestBody();
      var c = rb.getCaseDto();

      // 1) case 入力
      String caseType = trim(c.getCaseType());
      String category1DisplayId = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD
      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      // 2) 明细输入（summary1 可选，summary2 现状保持可选）
      String summary1 = trim(extractSummary1(rb));
      String summary2 = trim(extractSummary2(rb));

      String summary1Param = isBlank(summary1) ? "" : summary1;
      String summary2Param = isBlank(summary2) ? "" : summary2;

      // 3) 必填（现状保持：当作内部错误 => HTTP 500）
      if (isBlank(caseType)) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: caseType is required");
      }
      if (isBlank(category1DisplayId)) { // MOD
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: categoryLevel1.displayId is required"); // MOD
      }
      if (isBlank(txStr)) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error: extensions.TransactionDate is required");
      }

      // 4) Category2 必須判定（MOD: yml rules）
      boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId); // MOD

      if (needCategory2 && isBlank(category2DisplayId)) { // MOD
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
      }

      LocalDate txDate = DateParsers.parseFlexible(txStr);
      String category2Param = needCategory2 ? category2DisplayId : ""; // MOD

      // 5) 查 DB：取 SUMMARY3
      List<String> summary3List = repo.findDistinctSummary3(
          caseType,
          category1DisplayId, // MOD
          category2Param,     // MOD
          summary1Param,
          summary2Param,
          txDate,
          needCategory2);

      if (summary3List == null) {
        summary3List = Collections.emptyList();
      }

      // 6) SUMMARY3 重複排除 -> codes=[{"key": val}]
      List<CodeDto> codes = summary3List.stream()
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

  private String extractSummary1(Summary3Request.RequestBody rb) {
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return null;
    }
    for (var row : rb.getForm().getMeisai()) {
      if (row != null && !isBlank(row.getFSummary1()))
        return row.getFSummary1();
    }
    return null;
  }

  private String extractSummary2(Summary3Request.RequestBody rb) {
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return null;
    }
    for (var row : rb.getForm().getMeisai()) {
      if (row != null && !isBlank(row.getFSummary2()))
        return row.getFSummary2();
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

// import com.jt.summary.dto.Summary3Request;
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
// public class Summary3Service {

//   /**
//    * 固定資産（Fixed Asset）系の caseType。
//    * ※必要に応じて追加。
//    */
//   private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//       "ZD11");

//   // Category Level1
//   private static final String CATEGORY1_DOMESTIC = "国内";
//   private static final String CATEGORY1_FOREIGN = "海外";
//   private static final String CATEGORY1_OVERSEAS = "Overseas";
//   private static final String CATEGORY1_INCOME = "収入金計上";

//   /**
//    * Category2 を不要（無視）とする Category1 一覧
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

//   public SummaryResponse getSummary3(Summary3Request req) {

//     try {
//       // 0) NPE 防御（仕様：想定外は HTTP 500）
//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         throw new ResponseStatusException(
//             HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
//       }

//       var rb = req.getRequestBody();
//       var c = rb.getCaseDto();

//       // 1) case 入力（★normalizeは使わない）
//       String caseType = trim(c.getCaseType());
//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());
//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       // 2) 明细输入（summary1 可选，summary2 现状保持可选）
//       String summary1 = trim(extractSummary1(rb));
//       String summary2 = trim(extractSummary2(rb));

//       String summary1Param = isBlank(summary1) ? "" : summary1;
//       String summary2Param = isBlank(summary2) ? "" : summary2;

//       // 3) 必填（现状保持：当作内部错误 => HTTP 500）
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

//       // 4) Category2 必須判定（★固定資産は常に必須、normalize無し）
//       boolean needCategory2 = isCategory2Required(caseType, category1);

//       if (needCategory2 && isBlank(category2)) {
//         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
//       }

//       LocalDate txDate = DateParsers.parseFlexible(txStr);
//       String category2Param = needCategory2 ? category2 : "";

//       // 5) 查 DB：取 SUMMARY3
//       List<String> summary3List = repo.findDistinctSummary3(
//           caseType,
//           category1,
//           category2Param,
//           summary1Param,
//           summary2Param,
//           txDate,
//           needCategory2);

//       if (summary3List == null) {
//         summary3List = Collections.emptyList();
//       }

//       // 6) SUMMARY3 重複排除 -> codes=[{"key": val}]
//       List<CodeDto> codes = summary3List.stream()
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
//    * Category2必須判定：
//    * - 固定資産 caseType: 常に必須（国内/海外問わず）
//    * - それ以外：
//    * 国内 => 必須
//    * 海外/Overseas/収入金計上 => 不要
//    * それ以外/不明 => 必須（安全側）
//    */
//   private boolean isCategory2Required(String caseType, String category1) {
//     // 固定資産：国内/海外ともに Category2 必須
//     if (isFixedAssetCase(caseType)) {
//       return true;
//     }

//     String c1 = trim(category1);
//     if (c1 == null || c1.isBlank()) {
//       return true; // 安全側
//     }

//     // 国内 => need Category2
//     if (c1.startsWith(CATEGORY1_DOMESTIC)) {
//       return true;
//     }

//     if (CATEGORY1_IGNORE_CATEGORY2.stream().anyMatch(c1::startsWith)) {
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

//   private String extractSummary1(Summary3Request.RequestBody rb) {
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       return null;
//     }
//     for (var row : rb.getForm().getMeisai()) {
//       if (row != null && !isBlank(row.getFSummary1()))
//         return row.getFSummary1();
//     }
//     return null;
//   }

//   private String extractSummary2(Summary3Request.RequestBody rb) {
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       return null;
//     }
//     for (var row : rb.getForm().getMeisai()) {
//       if (row != null && !isBlank(row.getFSummary2()))
//         return row.getFSummary2();
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
