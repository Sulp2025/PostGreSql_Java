package com.jt.summary.service;

import com.jt.summary.dto.Summary5Request;
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
public class Summary5Service {

  // /summary2 と同じ Success 固定
  private static final String CODE_S000 = "S000";
  private static final String TYPE_INFO = "INFO";
  private static final String MSG_SUCCESS = "Success";

  private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD

  public SummaryResponse getSummary5(Summary5Request req) {

    try {

      // 0) 防御式检查（仕様：想定外は HTTP 500）
      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
      }

      var rb = req.getRequestBody();
      var c = rb.getCaseDto();

      // 1) case（变量名保持不动）
      String caseType = trim(c.getCaseType());
      String category1DisplayId = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD
      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      // 2) Form 明细（summary1~4）
      var row0 = getRow0(rb);

      // ✅ summary1 改为可选：空则视为“条件未指定”
      String summary1 = nullIfBlank(row0.getFSummary1());

      // summary2~4 は任意：空なら null にして「条件未指定」にする
      String summary2 = nullIfBlank(row0.getFSummary2());
      String summary3 = nullIfBlank(row0.getFSummary3());
      String summary4 = nullIfBlank(row0.getFSummary4());

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

      // MOD: yml rules
      boolean needCategory2 = ruleService.needCategory2(caseType, category1DisplayId); // MOD

      if (needCategory2 && isBlank(category2DisplayId)) { // MOD
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
      }

      LocalDate txDate = DateParsers.parseFlexible(txStr);
      String category2Param = needCategory2 ? category2DisplayId : ""; // MOD

      // 4) DB 查询 SUMMARY5（optional 条件版）
      List<String> list = repo.findDistinctSummary5Optional(
          caseType,
          category1DisplayId, // MOD
          category2Param,     // MOD
          summary1,
          summary2,
          summary3,
          summary4,
          txDate,
          needCategory2);

      if (list == null) {
        list = Collections.emptyList();
      }

      // 5) SUMMARY5 重複排除 -> codes=[{"key": val}]
      List<CodeDto> codes = list.stream()
          .filter(v -> v != null && !v.isBlank())
          .distinct()
          .map(v -> new CodeDto(v, ""))
          .collect(Collectors.toList());

      // 6) 成功固定（0件でも Success のまま）
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

  private Summary5Request.MeisaiRowDto getRow0(Summary5Request.RequestBody rb) {
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return new Summary5Request.MeisaiRowDto();
    }
    var row0 = rb.getForm().getMeisai().get(0);
    return row0 == null ? new Summary5Request.MeisaiRowDto() : row0;
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

// import com.jt.summary.dto.Summary5Request;
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
// public class Summary5Service {

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

//   // Income-related Category Level1
//   private static final String CATEGORY1_INCOME = "収入金計上";

//   /**
//    * Category2 を不要（無視）とする Category1 一覧
//    */
//   private static final Set<String> CATEGORY1_IGNORE_CATEGORY2 = Set.of(
//       CATEGORY1_FOREIGN,
//       CATEGORY1_OVERSEAS,
//       CATEGORY1_INCOME);

//   // /summary2 と同じ Success 固定
//   private static final String CODE_S000 = "S000";
//   private static final String TYPE_INFO = "INFO";
//   private static final String MSG_SUCCESS = "Success";

//   private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

//   private final TblSumRepository repo;

//   public SummaryResponse getSummary5(Summary5Request req) {

//     try {

//       // 0) 防御式检查（仕様：想定外は HTTP 500）
//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         throw new ResponseStatusException(
//             HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
//       }

//       var rb = req.getRequestBody();
//       var c = rb.getCaseDto();

//       // 1) case（变量名保持不动）※ normalize は使わない
//       String caseType = trim(c.getCaseType());
//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());
//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       // 2) Form 明细（summary1~4）
//       var row0 = getRow0(rb);

//       // ✅ summary1 改为可选：空则视为“条件未指定”
//       String summary1 = nullIfBlank(row0.getFSummary1());

//       // summary2~4 は任意：空なら null にして「条件未指定」にする
//       String summary2 = nullIfBlank(row0.getFSummary2());
//       String summary3 = nullIfBlank(row0.getFSummary3());
//       String summary4 = nullIfBlank(row0.getFSummary4());

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

//       // 规则：CATEGORY1_IGNORE_CATEGORY2 に含まれる場合は CATEGORY2 不要（基本ルール）
//       boolean needCategory2 = !CATEGORY1_IGNORE_CATEGORY2.contains(category1);

//       // ✅ 固定資産(ZD11) の場合：国内/海外 はどちらも category2 必須にする
//       if (CASETYPE_ASSET_CATE.contains(caseType)
//           && (CATEGORY1_DOMESTIC.equals(category1) || CATEGORY1_FOREIGN.equals(category1))) {
//         needCategory2 = true;
//       }

//       if (needCategory2 && isBlank(category2)) {
//         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
//       }

//       LocalDate txDate = DateParsers.parseFlexible(txStr);
//       String category2Param = needCategory2 ? category2 : "";

//       // 4) DB 查询 SUMMARY5（optional 条件版）
//       // summary1~4 が null の場合は「条件未指定」として repo 側（JPQL/SQL）で無視する想定
//       List<String> list = repo.findDistinctSummary5Optional(
//           caseType,
//           category1,
//           category2Param,
//           summary1,
//           summary2,
//           summary3,
//           summary4,
//           txDate,
//           needCategory2);

//       if (list == null) {
//         list = Collections.emptyList();
//       }

//       // 5) SUMMARY5 重複排除 -> codes=[{"key": val}]
//       List<CodeDto> codes = list.stream()
//           .filter(v -> v != null && !v.isBlank())
//           .distinct()
//           .map(v -> new CodeDto(v, ""))
//           .collect(Collectors.toList());

//       // 6) 成功固定（0件でも Success のまま）
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

//   private Summary5Request.MeisaiRowDto getRow0(Summary5Request.RequestBody rb) {
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       // 返回一个空对象，避免 NPE；真正必填校验在上面做
//       return new Summary5Request.MeisaiRowDto();
//     }
//     var row0 = rb.getForm().getMeisai().get(0);
//     return row0 == null ? new Summary5Request.MeisaiRowDto() : row0;
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
