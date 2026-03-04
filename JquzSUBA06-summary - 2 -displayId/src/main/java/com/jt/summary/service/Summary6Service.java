package com.jt.summary.service;

import com.jt.summary.dto.Summary6Request;
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
public class Summary6Service {

  // /summary2 と同じ Success 固定
  private static final String CODE_S000 = "S000";
  private static final String TYPE_INFO = "INFO";
  private static final String MSG_SUCCESS = "Success";

  private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

  private final TblSumRepository repo;

  private final Category2RuleService ruleService; // MOD

  public SummaryResponse getSummary6(Summary6Request req) {

    try {

      if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
      }

      var c = req.getRequestBody().getCaseDto();

      String caseType = trim(c.getCaseType());
      String category1DisplayId = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId()); // MOD
      String category2DisplayId = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId()); // MOD
      String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

      // 明细行：summary1~summary5（✅ summary1 も任意）
      String summary1 = nullIfBlank(extract(req, 1));
      String summary2 = nullIfBlank(extract(req, 2));
      String summary3 = nullIfBlank(extract(req, 3));
      String summary4 = nullIfBlank(extract(req, 4));
      String summary5 = nullIfBlank(extract(req, 5));

      // 基本校验（现状维持：缺的话当作异常 => HTTP 500）
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

      List<String> list = repo.findDistinctSummary6Optional(
          caseType,
          category1DisplayId, // MOD
          category2Param,     // MOD
          summary1,
          summary2,
          summary3,
          summary4,
          summary5,
          txDate,
          needCategory2);

      if (list == null) {
        list = Collections.emptyList();
      }

      // SUMMARY6 重複排除 -> codes=[{"key": val}]
      List<CodeDto> codes = list.stream()
          .filter(s -> s != null && !s.isBlank())
          .distinct()
          .map(s -> new CodeDto(s, ""))
          .collect(Collectors.toList());

      // 成功固定（0件でも Success のまま）
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

  private String extract(Summary6Request req, int n) {
    var rb = req.getRequestBody();
    if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
      return null;
    }
    var row0 = rb.getForm().getMeisai().get(0);
    if (row0 == null)
      return null;

    return switch (n) {
      case 1 -> row0.getFSummary1();
      case 2 -> row0.getFSummary2();
      case 3 -> row0.getFSummary3();
      case 4 -> row0.getFSummary4();
      case 5 -> row0.getFSummary5();
      default -> null;
    };
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

// import com.jt.summary.dto.Summary6Request;
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
// public class Summary6Service {

//   /**
//    * 固定資産（Fixed Asset）系の caseType。
//    */
//   private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//       "ZD11");
//   // Category Level1（最小追加：既存 FOREIGN を残しつつ拡張）
//   private static final String DOMESTIC = "国内";
//   private static final String FOREIGN = "海外";
//   private static final String OVERSEAS = "Overseas";
//   private static final String INCOME_BOOKING = "収入金計上";

//   /**
//    * Category2 を不要（無視）とする Category1 一覧
//    * （ただし固定資産 caseType の場合は海外/Overseasでも category2 必須）
//    */
//   private static final Set<String> CATEGORY1_IGNORE_CATEGORY2 = Set.of(
//       FOREIGN,
//       OVERSEAS,
//       INCOME_BOOKING);

//   // /summary2 と同じ Success 固定
//   private static final String CODE_S000 = "S000";
//   private static final String TYPE_INFO = "INFO";
//   private static final String MSG_SUCCESS = "Success";

//   private static final String MSG_MISSING_CATEGORY2 = "Please select Category Level2.";

//   private final TblSumRepository repo;

//   public SummaryResponse getSummary6(Summary6Request req) {

//     try {

//       if (req == null || req.getRequestBody() == null || req.getRequestBody().getCaseDto() == null) {
//         // 仕様：想定外は HTTP 500
//         throw new ResponseStatusException(
//             HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: requestBody.case is required");
//       }

//       var c = req.getRequestBody().getCaseDto();

//       String caseType = trim(c.getCaseType());
//       String category1 = trim(c.getCategoryLevel1() == null ? null : c.getCategoryLevel1().getDisplayId());
//       String category2 = trim(c.getCategoryLevel2() == null ? null : c.getCategoryLevel2().getDisplayId());
//       String txStr = trim(c.getExtensions() == null ? null : c.getExtensions().getTransactionDate());

//       // 明细行：summary1~summary5（✅ summary1 も任意）
//       String summary1 = nullIfBlank(extract(req, 1));
//       String summary2 = nullIfBlank(extract(req, 2));
//       String summary3 = nullIfBlank(extract(req, 3));
//       String summary4 = nullIfBlank(extract(req, 4));
//       String summary5 = nullIfBlank(extract(req, 5));

//       // 基本校验（现状维持：缺的话当作异常 => HTTP 500）
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

//       // 规则：
//       // - 通常：CATEGORY1_IGNORE_CATEGORY2 に含まれる場合は CATEGORY2 不要
//       // - 固定資産（ZD11）の場合：category1 が 国内/海外/Overseas なら CATEGORY2 必須（=> 海外/Overseas
//       // の例外上書き）
//       boolean isAssetCase = CASETYPE_ASSET_CATE.contains(caseType);

//       boolean needCategory2 = !CATEGORY1_IGNORE_CATEGORY2.contains(category1);
//       if (isAssetCase && (DOMESTIC.equals(category1) || FOREIGN.equals(category1) || OVERSEAS.equals(category1))) {
//         needCategory2 = true;
//       }

//       if (needCategory2 && isBlank(category2)) {
//         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MSG_MISSING_CATEGORY2);
//       }

//       LocalDate txDate = DateParsers.parseFlexible(txStr);
//       String category2Param = needCategory2 ? category2 : "";

//       List<String> list = repo.findDistinctSummary6Optional(
//           caseType,
//           category1,
//           category2Param,
//           summary1,
//           summary2,
//           summary3,
//           summary4,
//           summary5,
//           txDate,
//           needCategory2);

//       if (list == null) {
//         list = Collections.emptyList();
//       }

//       // SUMMARY6 重複排除 -> codes=[{"key": val}]
//       List<CodeDto> codes = list.stream()
//           .filter(s -> s != null && !s.isBlank())
//           .distinct()
//           .map(s -> new CodeDto(s, "")) // 只显示 key
//           .collect(Collectors.toList());

//       // 成功固定（0件でも Success のまま）
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

//   private String extract(Summary6Request req, int n) {
//     var rb = req.getRequestBody();
//     if (rb.getForm() == null || rb.getForm().getMeisai() == null || rb.getForm().getMeisai().isEmpty()) {
//       return null;
//     }
//     var row0 = rb.getForm().getMeisai().get(0);
//     if (row0 == null)
//       return null;

//     return switch (n) {
//       case 1 -> row0.getFSummary1();
//       case 2 -> row0.getFSummary2();
//       case 3 -> row0.getFSummary3();
//       case 4 -> row0.getFSummary4();
//       case 5 -> row0.getFSummary5();
//       default -> null;
//     };
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
