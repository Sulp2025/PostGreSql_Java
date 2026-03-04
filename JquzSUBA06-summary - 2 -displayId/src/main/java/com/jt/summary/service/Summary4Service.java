package com.jt.summary.service;

import com.jt.summary.dto.Summary4Request;
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
public class Summary4Service {

  private static final String CODE_S000 = "S000";
  private static final String TYPE_INFO = "INFO";
  private static final String MSG_SUCCESS = "Success";

  // missing は HTTP 400
  private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD

  public SummaryResponse getSummary4(Summary4Request req) {

    try {

      // 0) 防御式检查（仕様：想定外は HTTP 500）
      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
      }

      var rb = req.getRequestBody();
      var c = rb.getCaseDto();

      // 1) case 入力（变量名保持不动）
      String caseType = trim(c.getCaseType());
      String category1DisplayId = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD
      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      // 2) 明细行输入：Summary1/2/3
      var row0 = getRow0(rb);

      String summary1 = nullIfBlank(row0.getFSummary1());
      String summary2 = nullIfBlank(row0.getFSummary2());
      String summary3 = nullIfBlank(row0.getFSummary3());

      // 3) 基本校验（现状维持：缺的话当作异常 => HTTP 500）
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

      // 4) Category2 必须性规则（MOD: yml rules）
      boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId); // MOD

      if (needCategory2 && isBlank(category2DisplayId)) { // MOD
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
      }

      // Category2 不需要时传空字符串
      String category2Param = needCategory2 ? category2DisplayId : ""; // MOD

      LocalDate txDate = DateParsers.parseFlexible(txStr);

      // 5) 查 DB：取 SUMMARY4 下拉（optional 条件版）
      List<String> summary4List = repo.findDistinctSummary4Optional(
          caseType,
          category1DisplayId, // MOD
          category2Param,     // MOD
          summary1,
          summary2,
          summary3,
          txDate,
          needCategory2);

      if (summary4List == null) {
        summary4List = Collections.emptyList();
      }

      // 6) SUMMARY4 重複排除 -> codes=[{"key": val}]
      List<CodeDto> codes = summary4List.stream()
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

  private Summary4Request.MeisaiRowDto getRow0(Summary4Request.RequestBody rb) {
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return new Summary4Request.MeisaiRowDto();
    }
    var row0 = rb.getForm().getMeisai().get(0);
    return row0 == null ? new Summary4Request.MeisaiRowDto() : row0;
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private String trim(String s) {
    return s == null ? null : s.trim();
  }

  private String nullIfBlank(String s) {
    if (s == null)
      return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}



// package com.jt.summary.service;

// import com.jt.summary.dto.Summary4Request;
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
// public class Summary4Service {

//   /**
//    * 固定資産（Fixed Asset）系の caseType。
//    * ※必要に応じて追加。
//    */
//   private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//       "ZD11");

//   // Category Level1
//   private static final String CATEGORY1_DOMESTIC = "国内";
//   private static final String CATEGORY1_FOREIGN = "海外";
//     // private static final String CATEGORY1_FOREIGN = "Z02000000";
//   private static final String CATEGORY1_OVERSEAS = "Overseas";

//   // Income-related Category Level1
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

//   // missing は HTTP 400
//   private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

//   private final TblSumRepository repo;

//   public SummaryResponse getSummary4(Summary4Request req) {

//     try {

//       // 0) 防御式检查（仕様：想定外は HTTP 500）
//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         throw new ResponseStatusException(
//             HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
//       }

//       var rb = req.getRequestBody();
//       var c = rb.getCaseDto();

//       // 1) case 入力（变量名保持不动）
//       String caseType = trim(c.getCaseType());
//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());
//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       // 2) 明细行输入：Summary1/2/3
//       var row0 = getRow0(rb);

//       String summary1 = nullIfBlank(row0.getFSummary1());
//       String summary2 = nullIfBlank(row0.getFSummary2());
//       String summary3 = nullIfBlank(row0.getFSummary3());

//       // 3) 基本校验（现状维持：缺的话当作异常 => HTTP 500）
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

//       // 4) Category2 必须性规则：
//       // - 国内：Category2 必须
//       // - CATEGORY1_IGNORE_CATEGORY2：Category2 不需要
//       // - 其它：Category2 必须
//       String baseCategory1 = category1;

//       boolean needCategory2 = (baseCategory1 != null && baseCategory1.startsWith(CATEGORY1_DOMESTIC))
//           || !CATEGORY1_IGNORE_CATEGORY2.contains(baseCategory1);

//       // 追加：固定资产ZD11 の場合、海外/Overseas でも Category2 必須にする
//       if (CASETYPE_ASSET_CATE.contains(caseType)
//           && (CATEGORY1_FOREIGN.equals(baseCategory1) || CATEGORY1_OVERSEAS.equals(baseCategory1))) {
//         needCategory2 = true;
//       }

//       if (needCategory2 && isBlank(category2)) {
//         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
//       }

//       // Category2 不需要时传空字符串
//       String category2Param = needCategory2 ? category2 : "";

//       LocalDate txDate = DateParsers.parseFlexible(txStr);

//       // 5) 查 DB：取 SUMMARY4 下拉（optional 条件版）
//       // summary1/2/3 が null の場合は「条件未指定」として repo 側（JPQL/SQL）で無視する想定
//       List<String> summary4List = repo.findDistinctSummary4Optional(
//           caseType,
//           baseCategory1,
//           category2Param,
//           summary1,
//           summary2,
//           summary3,
//           txDate,
//           needCategory2);

//       if (summary4List == null) {
//         summary4List = Collections.emptyList();
//       }

//       // 6) SUMMARY4 重複排除 -> codes=[{"key": val}]
//       List<CodeDto> codes = summary4List.stream()
//           .filter(s -> s != null && !s.isBlank())
//           .distinct()
//           .map(s -> new CodeDto(s, ""))
//           .collect(Collectors.toList());

//       // 7) 成功固定（0件でも Success のまま）
//       return buildSuccess(codes);

//     } catch (ResponseStatusException ex) {
//       throw ex;
//     } catch (Exception ex) {
//       // 想定外例外：HTTP 500 detail "Internal Server Error: ..."
//       throw new ResponseStatusException(
//           HttpStatus.INTERNAL_SERVER_ERROR,
//           "Internal Server Error: " + safeExString(ex),
//           ex);
//     }
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

//   private Summary4Request.MeisaiRowDto getRow0(Summary4Request.RequestBody rb) {
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       return new Summary4Request.MeisaiRowDto();
//     }
//     var row0 = rb.getForm().getMeisai().get(0);
//     return row0 == null ? new Summary4Request.MeisaiRowDto() : row0;
//   }

//   private boolean isBlank(String s) {
//     return s == null || s.trim().isEmpty();
//   }

//   private String trim(String s) {
//     return s == null ? null : s.trim();
//   }

//   private String nullIfBlank(String s) {
//     if (s == null)
//       return null;
//     String t = s.trim();
//     return t.isEmpty() ? null : t;
//   }
// }
