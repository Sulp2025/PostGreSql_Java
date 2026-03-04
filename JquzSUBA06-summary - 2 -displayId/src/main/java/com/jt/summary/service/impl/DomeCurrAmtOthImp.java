package com.jt.summary.service.impl;

import com.jt.domesticamount.dto.DomesticAmountRequest;
import com.jt.domesticamount.dto.DomesticAmountResponse;
import com.jt.domesticamount.dto.DomesticAmountResponse.CodeItem;
import com.jt.domesticamount.dto.FormModels;
import com.jt.domesticamount.dto.FormModels.MeisaiItem;
import com.jt.domesticamount.service.DomesticAmountService;
import com.jt.summary.config.AppProperties; // MOD
import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.CheckResponse;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.DomeCurrAmtOthService;
import com.jt.summary.util.DateParsers;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class DomeCurrAmtOthImp implements DomeCurrAmtOthService {

  // MOD: category2 requirement is driven by application.yml (AppProperties)

  private final DomesticAmountService service; // jar：国内通貨額(税抜)算出
  private final TblSumRepository tblSumRepository;
  private final AppProperties appProperties; // MOD

  @Override
  public CheckResponse checkDomesticUnitPrice(AccountCodeRequest req) {

    List<String> errors = new ArrayList<>();

    AccountCodeRequest.RequestBody body = (req != null) ? req.getRequestBody() : null;
    AccountCodeRequest.FormDto form = (body != null) ? body.getForm() : null;
    AccountCodeRequest.CaseDto c = (body != null) ? body.getCaseDto() : null;

    if (body == null)
      return CheckResponse.fromCheckResult(true, "requestBody が存在しません。");
    if (form == null)
      return CheckResponse.fromCheckResult(true, "requestBody.Form が存在しません。");
    if (c == null)
      return CheckResponse.fromCheckResult(true, "requestBody.case が存在しません。");

    List<AccountCodeRequest.MeisaiRowDto> meisai =
        (form.getMeisai() != null) ? form.getMeisai() : new ArrayList<>();

    // ===== header（master検索に追加する条件）=====
    String caseType = trimToNull(c.getCaseType());

    String category1DisplayId =
        (c.getCategoryLevel1() != null)
            ? trimToNull(c.getCategoryLevel1().getDisplayId())
            : null; // MOD: use displayId
    String category2DisplayId =
        (c.getCategoryLevel2() != null)
            ? trimToNull(c.getCategoryLevel2().getDisplayId())
            : null; // MOD: use displayId

    LocalDate txDate = resolveTransactionDate(c);

    // MOD: category2 必須/任意 は application.yml(app.casetype-rules) に従う
    boolean needCategory2 = true; // safe default // MOD
    if (!isBlank(caseType) && appProperties != null && appProperties.getCasetypeRules() != null) { // MOD
      AppProperties.CaseTypeRule rule = appProperties.getCasetypeRules().get(caseType); // MOD
      if (rule != null) { // MOD
        needCategory2 = rule.isDefaultRequireCategory2(); // MOD
        if (category1DisplayId != null && rule.getCategory1Overrides() != null) { // MOD
          Boolean ov = rule.getCategory1Overrides().get(category1DisplayId); // MOD
          if (ov != null) needCategory2 = ov; // MOD
        }
      }
    }

    String category2Param = needCategory2 ? trimToNull(category2DisplayId) : null; // MOD

    // =========================================================
    // category2 必須ルール
    // =========================================================
    if (needCategory2 && category2Param == null) {
      return CheckResponse.fromCheckResult(
          true,
          "category2 は必須です。（caseType=" + (caseType == null ? "" : caseType)
              + ", category1_displayId=" + (category1DisplayId == null ? "" : category1DisplayId) + "）"); // MOD
    }

    // =========================================================
    // CHECK A/B：ヘッダチェック（fail-fast ではなくエラー蓄積）
    // =========================================================
    List<String> headerErrors = new ArrayList<>();

    addHeaderTextErrors(headerErrors, form.getFSettlement1(), "決済番号1");
    addHeaderTextErrors(headerErrors, form.getFSettlement2(), "決済番号2");
    addHeaderTextErrors(headerErrors, form.getFSettlement3(), "決済番号3");
    addHeaderTextErrors(headerErrors, form.getFInvoiceNumber(), "請求書番号");

    // 合計金額：空OK、半角整数のみ
    String totalAmt = form.getFTotalAmountC();
    if (!isBlank(totalAmt) && !isHalfWidthInteger(totalAmt)) {
      headerErrors.add("合計金額 は半角整数で入力してください。");
    }

    errors.addAll(headerErrors);

    // =========================================================
    // 数値エラー行は後続チェックをスキップ
    // =========================================================
    Set<Integer> rowsWithNumericError = new HashSet<>();
    Map<Integer, BigDecimal> domeAmtByRow = new HashMap<>();

    BigDecimal rate = form.getFRate(); // jar算出が必要な時だけ使う

    int rowNo = 0;
    for (AccountCodeRequest.MeisaiRowDto row : meisai) {
      rowNo++;

      // 保存前の全量チェック対応
      if (row == null) {
        errors.add("Row " + rowNo + ": 数量 は必須項目です。");
        errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
        rowsWithNumericError.add(rowNo);
        continue;
      }

      String qtyRaw = row.getFQuantity1();
      String docAmtRaw = row.getFDocCurrAmt();
      String domeAmtRaw = row.getFDomeCurrAmt();

      // 数量：必須 / 半角整数 / >=1
      if (isBlank(qtyRaw)) {
        errors.add("Row " + rowNo + ": 数量 は必須項目です。");
        rowsWithNumericError.add(rowNo);
      } else if (!isHalfWidthInteger(qtyRaw)) {
        errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
        rowsWithNumericError.add(rowNo);
      } else {
        try {
          BigDecimal qty = new BigDecimal(qtyRaw.trim());
          if (qty.compareTo(BigDecimal.ONE) < 0) {
            errors.add("Row " + rowNo + ": 数量 は1以上で入力してください。");
            rowsWithNumericError.add(rowNo);
          }
        } catch (Exception e) {
          errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
          rowsWithNumericError.add(rowNo);
        }
      }

      // 伝票通貨額：必須 / 半角整数
      BigDecimal docAmt = null;
      if (isBlank(docAmtRaw)) {
        errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
        rowsWithNumericError.add(rowNo);
      } else if (!isHalfWidthInteger(docAmtRaw)) {
        errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
        rowsWithNumericError.add(rowNo);
      } else {
        try {
          docAmt = new BigDecimal(docAmtRaw.trim());
        } catch (Exception e) {
          errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
          rowsWithNumericError.add(rowNo);
        }
      }

      // 国内通貨額(税抜)：画面値優先、なければ jar 算出（rate 必須）
      if (!rowsWithNumericError.contains(rowNo)) {

        BigDecimal domeAmt = null;

        // 下拉未选中(空)时，先出「半角整数」メッセージに統一
        if (isBlank(domeAmtRaw)) {
          errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
        } else {
          if (!isHalfWidthInteger(domeAmtRaw)) {
            errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
            rowsWithNumericError.add(rowNo);
          } else {
            try {
              domeAmt = new BigDecimal(domeAmtRaw.trim());
              domeAmtByRow.put(rowNo, domeAmt);
            } catch (Exception e) {
              errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
              rowsWithNumericError.add(rowNo);
            }
          }
        }

        if (!rowsWithNumericError.contains(rowNo) && domeAmt == null) {
          // jar算出
          if (rate == null) {
            errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（レート が未設定）。");
            rowsWithNumericError.add(rowNo);
          } else if (docAmt == null) {
            errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（伝票通貨額 が不正）。");
            rowsWithNumericError.add(rowNo);
          } else {
            try {
              BigDecimal calculated = calcDomesticNetTaxExcludedByJar(docAmt, rate);
              domeAmtByRow.put(rowNo, calculated);
            } catch (Exception ex) {
              errors.add("Row " + rowNo + ": 国内通貨額(税抜) の算出に失敗しました。（" + ex.getMessage() + "）");
              rowsWithNumericError.add(rowNo);
            }
          }
        }
      }

      String ac = nullIfBlank(row.getFAccountCode());
      if (ac == null) {
        errors.add("Row " + rowNo + ": 勘定コードは必須です。");
      }
    }

    // =========================================================
    // 金額範囲チェック（国内通貨額/数量）
    // =========================================================
    rowNo = 0;
    for (AccountCodeRequest.MeisaiRowDto row : meisai) {
      rowNo++;

      if (row == null) continue;

      String qtyRaw = row.getFQuantity1();
      BigDecimal qty;
      try {
        qty = new BigDecimal(qtyRaw.trim());
      } catch (Exception e) {
        errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
        continue;
      }
      if (qty.compareTo(BigDecimal.ZERO) <= 0) {
        errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
        continue;
      }

      BigDecimal domeAmt = domeAmtByRow.get(rowNo);
      if (domeAmt == null) {
        errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
        continue;
      }

      BigDecimal checkAmount;
      try {
        checkAmount = domeAmt.divide(qty, 6, RoundingMode.HALF_UP).stripTrailingZeros();
      } catch (Exception e) {
        errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
        continue;
      }

      //  summary1～6 すべて任意：空は null（＝条件未指定）
      String s1 = nullIfBlank(row.getFSummary1());
      String s2 = nullIfBlank(row.getFSummary2());
      String s3 = nullIfBlank(row.getFSummary3());
      String s4 = nullIfBlank(row.getFSummary4());
      String s5 = nullIfBlank(row.getFSummary5());
      String s6 = nullIfBlank(row.getFSummary6());

      String accountCode = nullIfBlank(row.getFAccountCode());
      String enterExpenses = nullIfBlank(row.getFEnterExpenses());

      if (accountCode == null) continue;

      BigDecimal max =
          tblSumRepository.findMaxAmountWithFilters(
              caseType, category1DisplayId, category2Param, // MOD
              s1, s2, s3, s4, s5, s6,
              accountCode, enterExpenses,
              txDate, needCategory2);

      BigDecimal min =
          tblSumRepository.findMinAmountWithFilters(
              caseType, category1DisplayId, category2Param, // MOD
              s1, s2, s3, s4, s5, s6,
              accountCode, enterExpenses,
              txDate, needCategory2);

      if (min == null || max == null) {
        errors.add("Row " + rowNo + ": 金額範囲が特定できません(摘要内容および交際費区分をご確認ください)");
        continue;
      }

      if (checkAmount.compareTo(min) < 0 || checkAmount.compareTo(max) > 0) {
        errors.add(
            "Row " + rowNo + ": 国内通貨額/数量 = " + disp(checkAmount)
                + " is out of allowed range (" + disp(min) + " - " + disp(max) + ").");
      }
    }

    if (CollectionUtils.isEmpty(meisai)) {
      errors.add("明細なし");
    }

    if (!errors.isEmpty()) {
      return CheckResponse.fromCheckResult(true, String.join("\n", errors));
    }

    return CheckResponse.fromCheckResult(false, "エラーなし");
  }

  private BigDecimal calcDomesticNetTaxExcludedByJar(BigDecimal docCurrAmt, BigDecimal rate) {

    DomesticAmountRequest request = new DomesticAmountRequest();
    DomesticAmountRequest.RequestBody requestBody = new DomesticAmountRequest.RequestBody();

    FormModels.Form form1 = new FormModels.Form();

    MeisaiItem meisaiItem = new MeisaiItem();
    meisaiItem.setDocCurrAmt(docCurrAmt);

    List<MeisaiItem> meisaiList = new ArrayList<>();
    meisaiList.add(meisaiItem);

    form1.setMeisai(meisaiList);
    form1.setRate(rate);

    requestBody.setForm(form1);
    request.setRequestBody(requestBody);

    DomesticAmountResponse resp = service.calculate(request);

    if (resp == null
        || resp.getResponseBody() == null
        || resp.getResponseBody().getValue() == null) {
      throw new IllegalStateException("国内通貨額計算の戻り値が不正です。");
    }

    List<CodeItem> codes = resp.getResponseBody().getValue().getCodes();
    if (ObjectUtils.isEmpty(codes) || codes.get(0) == null || isBlank(codes.get(0).getKey())) {
      throw new IllegalStateException("国内通貨額計算の戻り値 が空です。");
    }

    return new BigDecimal(codes.get(0).getKey().trim());
  }

  private static LocalDate resolveTransactionDate(AccountCodeRequest.CaseDto c) {
    if (c == null || c.getExtensions() == null)
      return null;

    Object ext = c.getExtensions();
    try {
      Method m = ext.getClass().getMethod("getTransactionDate");
      Object v = m.invoke(ext);

      if (v == null)
        return null;
      if (v instanceof LocalDate ld)
        return ld;

      if (v instanceof String s) {
        String raw = s.trim();
        if (raw.isEmpty())
          return null;
        return DateParsers.parseFlexible(raw);
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private static void addHeaderTextErrors(List<String> errors, String value, String label) {
    if (isBlank(value))
      return;
    if (containsWideChar(value))
      errors.add(label + " に全角文字が含まれています。");
    if (containsForbiddenChar(value))
      errors.add(label + " に禁則文字が含まれています。");
  }

  private static boolean containsWideChar(String s) {
    return s != null && s.codePoints().anyMatch(cp -> cp > 0x7E);
  }

  private static boolean containsForbiddenChar(String s) {
    if (s == null)
      return false;
    final int[] forbidden = new int[] { '"', '\'', '\\', '/', '<', '>', '|', ':', '*', '?', '、' };
    return s.codePoints().anyMatch(cp -> {
      for (int f : forbidden)
        if (cp == f)
          return true;
      return false;
    });
  }

  @SuppressWarnings("unused")
  private static boolean isAmountInputStarted(AccountCodeRequest.MeisaiRowDto row) {
    if (row == null)
      return false;
    return !isBlank(row.getFQuantity1())
        || !isBlank(row.getFDocCurrAmt())
        || !isBlank(row.getFDomeCurrAmt());
  }

  private static boolean isHalfWidthInteger(String s) {
    if (s == null)
      return false;
    String t = s.trim();
    return !t.isEmpty() && t.matches("^[0-9]+$");
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String nullIfBlank(String s) {
    if (s == null)
      return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String trimToNull(String s) {
    return nullIfBlank(s);
  }

  private static String disp(BigDecimal v) {
    if (v == null)
      return "";
    return v.stripTrailingZeros().toPlainString();
  }
}



// package com.jt.summary.service.impl;

// import com.jt.summary.config.AppProperties; // MOD: add
// import com.jt.domesticamount.dto.DomesticAmountRequest;
// import com.jt.domesticamount.dto.DomesticAmountResponse;
// import com.jt.domesticamount.dto.DomesticAmountResponse.CodeItem;
// import com.jt.domesticamount.dto.FormModels;
// import com.jt.domesticamount.dto.FormModels.MeisaiItem;
// import com.jt.domesticamount.service.DomesticAmountService;
// import com.jt.summary.dto.AccountCodeRequest;
// import com.jt.summary.dto.CheckResponse;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.service.DomeCurrAmtOthService;
// import com.jt.summary.util.DateParsers;
// import java.lang.reflect.Method;
// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.LocalDate;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.util.CollectionUtils;
// import org.springframework.util.ObjectUtils;

// @Service
// @RequiredArgsConstructor
// public class DomeCurrAmtOthImp implements DomeCurrAmtOthService {

//   // private static final Set<String> CASETYPE_ASSET_CATE = Set.of("ZD11");

//   // //: Category Level1
//   // private static final String CATEGORY1_DOMESTIC = "国内";
//   // private static final String CATEGORY1_FOREIGN = "海外";
//   // private static final String CATEGORY1_OVERSEAS = "Overseas";
//   // private static final String CATEGORY1_INCOME = "収入金計上";

//   // //: category1 が「海外 / Overseas / 収入金計上」の場合、通常 category2 は “空扱い”
//   // // category1 が上記以外（＝国内想定）の場合、category2 は必須
//   // // ただし固定資産(ZD11)かつ category1=国内/海外 の場合は category2 必須
//   // private static final Set<String> OVERSEAS_CATEGORIES =
//   // Set.of(CATEGORY1_FOREIGN, CATEGORY1_OVERSEAS, CATEGORY1_INCOME); //

//   private final AppProperties appProperties; // MOD: inject rules from application.yml

//   private final DomesticAmountService service; // jar：国内通貨額(税抜)算出
//   private final TblSumRepository tblSumRepository;

//   @Override
//   public CheckResponse checkDomesticUnitPrice(AccountCodeRequest req) {

//     List<String> errors = new ArrayList<>();

//     AccountCodeRequest.RequestBody body = (req != null) ? req.getRequestBody() : null;
//     AccountCodeRequest.FormDto form = (body != null) ? body.getForm() : null;
//     AccountCodeRequest.CaseDto c = (body != null) ? body.getCaseDto() : null;

//     if (body == null)
//       return CheckResponse.fromCheckResult(true, "requestBody が存在しません。");
//     if (form == null)
//       return CheckResponse.fromCheckResult(true, "requestBody.Form が存在しません。");
//     if (c == null)
//       return CheckResponse.fromCheckResult(true, "requestBody.case が存在しません。");

//     List<AccountCodeRequest.MeisaiRowDto> meisai = (form.getMeisai() != null) ? form.getMeisai() : new ArrayList<>();

//     // ===== header（master検索に追加する条件）=====
//     // String caseType = trimToNull(c.getCaseType());

//     // String category1 =
//     // (c.getCategoryLevel1() != null)
//     // ? trimToNull(c.getCategoryLevel1().getName())
//     // : null; //
//     // String category2 =
//     // (c.getCategoryLevel2() != null)
//     // ? trimToNull(c.getCategoryLevel2().getName())
//     // : null;

//     // LocalDate txDate = resolveTransactionDate(c);

//     // boolean isAssetCaseType = (caseType != null) &&
//     // CASETYPE_ASSET_CATE.contains(caseType);

//     // boolean needCategory2 = false;
//     // if (category1 != null) {
//     // if (CATEGORY1_DOMESTIC.equals(category1)) {
//     // // 国内 => 必須（ZD11でも必須）
//     // needCategory2 = true;
//     // } else if (OVERSEAS_CATEGORIES.contains(category1)) {
//     // // 海外/Overseas/収入金計上 => 原則不要
//     // // ただし ZD11 かつ 海外 の場合は必須
//     // needCategory2 = isAssetCaseType && CATEGORY1_FOREIGN.equals(category1);
//     // } else {
//     // // 想定外の category1 は安全側で必須扱い
//     // needCategory2 = true;
//     // }
//     // }

//     // String category2Param = needCategory2 ? trimToNull(category2) : null;
//     // =========================================================
//     // MOD: category2 required rule -> read from app.casetype-rules in
//     // application.yml
//     // - defaultRequireCategory2
//     // - category1Overrides[category1_displayid]
//     // =========================================================
//     boolean needCategory2 = true; // MOD: safe default
//     if (caseType != null && appProperties != null && appProperties.getCasetypeRules() != null) { // MOD
//       AppProperties.CaseTypeRule rule = appProperties.getCasetypeRules().get(caseType); // MOD
//       if (rule != null) { // MOD
//         needCategory2 = rule.isDefaultRequireCategory2(); // MOD
//         if (category1DisplayId != null && rule.getCategory1Overrides() != null) { // MOD
//           Boolean ov = rule.getCategory1Overrides().get(category1DisplayId); // MOD
//           if (ov != null)
//             needCategory2 = ov; // MOD
//         }
//       }
//     }

//     String category2Param = needCategory2 ? trimToNull(category2DisplayId) : null; // MOD

//     // =========================================================
//     // category2 必須ルール
//     // =========================================================
//     // if (needCategory2 && category2Param == null) {
//     // return CheckResponse.fromCheckResult(
//     // true,
//     // "category1 が「" + (category1 == null ? "" : category1) + "」の場合、category2
//     // は必須です。");
//     // }
//     if (needCategory2 && category2Param == null) {
//       return CheckResponse.fromCheckResult(
//           true,
//           "category2 は必須です。（caseType=" + (caseType == null ? "" : caseType)
//               + ", category1_displayId=" + (category1DisplayId == null ? "" : category1DisplayId) + "）"); // MOD
//     }

//     // =========================================================
//     // CHECK A/B：ヘッダチェック（fail-fast ではなくエラー蓄積）
//     // =========================================================
//     List<String> headerErrors = new ArrayList<>();

//     addHeaderTextErrors(headerErrors, form.getFSettlement1(), "決済番号1");
//     addHeaderTextErrors(headerErrors, form.getFSettlement2(), "決済番号2");
//     addHeaderTextErrors(headerErrors, form.getFSettlement3(), "決済番号3");
//     addHeaderTextErrors(headerErrors, form.getFInvoiceNumber(), "請求書番号");

//     // 合計金額：空OK、半角整数のみ
//     String totalAmt = form.getFTotalAmountC();
//     if (!isBlank(totalAmt) && !isHalfWidthInteger(totalAmt)) {
//       headerErrors.add("合計金額 は半角整数で入力してください。");
//     }

//     errors.addAll(headerErrors);

//     // =========================================================
//     // 数値エラー行は後続チェックをスキップ
//     // =========================================================
//     Set<Integer> rowsWithNumericError = new HashSet<>();
//     Map<Integer, BigDecimal> domeAmtByRow = new HashMap<>();

//     BigDecimal rate = form.getFRate(); // jar算出が必要な時だけ使う

//     int rowNo = 0;
//     for (AccountCodeRequest.MeisaiRowDto row : meisai) {
//       rowNo++;

//       // 保存前の全量チェック対応
//       if (row == null) {
//         errors.add("Row " + rowNo + ": 数量 は必須項目です。");
//         errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
//         rowsWithNumericError.add(rowNo);
//         continue;
//       }

//       String qtyRaw = row.getFQuantity1();
//       String docAmtRaw = row.getFDocCurrAmt();
//       String domeAmtRaw = row.getFDomeCurrAmt();

//       // 数量：必須 / 半角整数 / >=1
//       if (isBlank(qtyRaw)) {
//         errors.add("Row " + rowNo + ": 数量 は必須項目です。");
//         rowsWithNumericError.add(rowNo);
//       } else if (!isHalfWidthInteger(qtyRaw)) {
//         errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
//         rowsWithNumericError.add(rowNo);
//       } else {
//         try {
//           BigDecimal qty = new BigDecimal(qtyRaw.trim());
//           if (qty.compareTo(BigDecimal.ONE) < 0) {
//             errors.add("Row " + rowNo + ": 数量 は1以上で入力してください。");
//             rowsWithNumericError.add(rowNo);
//           }
//         } catch (Exception e) {
//           errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
//           rowsWithNumericError.add(rowNo);
//         }
//       }

//       // 伝票通貨額：必須 / 半角整数
//       BigDecimal docAmt = null;
//       if (isBlank(docAmtRaw)) {
//         errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
//         rowsWithNumericError.add(rowNo);
//       } else if (!isHalfWidthInteger(docAmtRaw)) {
//         errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
//         rowsWithNumericError.add(rowNo);
//       } else {
//         try {
//           docAmt = new BigDecimal(docAmtRaw.trim());
//         } catch (Exception e) {
//           errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
//           rowsWithNumericError.add(rowNo);
//         }
//       }

//       // 国内通貨額(税抜)：画面値優先、なければ jar 算出（rate 必須）
//       if (!rowsWithNumericError.contains(rowNo)) {

//         BigDecimal domeAmt = null;

//         // 下拉未选中(空)时，先出「半角整数」メッセージに統一
//         if (isBlank(domeAmtRaw)) {
//           errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
//         } else {
//           if (!isHalfWidthInteger(domeAmtRaw)) {
//             errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
//             rowsWithNumericError.add(rowNo);
//           } else {
//             try {
//               domeAmt = new BigDecimal(domeAmtRaw.trim());
//               domeAmtByRow.put(rowNo, domeAmt);
//             } catch (Exception e) {
//               errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
//               rowsWithNumericError.add(rowNo);
//             }
//           }
//         }

//         if (!rowsWithNumericError.contains(rowNo) && domeAmt == null) {
//           // jar算出
//           if (rate == null) {
//             errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（レート が未設定）。");
//             rowsWithNumericError.add(rowNo);
//           } else if (docAmt == null) {
//             errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（伝票通貨額 が不正）。");
//             rowsWithNumericError.add(rowNo);
//           } else {
//             try {
//               BigDecimal calculated = calcDomesticNetTaxExcludedByJar(docAmt, rate);
//               domeAmtByRow.put(rowNo, calculated);
//             } catch (Exception ex) {
//               errors.add("Row " + rowNo + ": 国内通貨額(税抜) の算出に失敗しました。（" + ex.getMessage() + "）");
//               rowsWithNumericError.add(rowNo);
//             }
//           }
//         }
//       }

//       String ac = nullIfBlank(row.getFAccountCode());
//       if (ac == null) {
//         errors.add("Row " + rowNo + ": 勘定コードは必須です。");
//       }
//     }

//     // =========================================================
//     // 金額範囲チェック（国内通貨額/数量）
//     // =========================================================
//     rowNo = 0;
//     for (AccountCodeRequest.MeisaiRowDto row : meisai) {
//       rowNo++;

//       if (row == null)
//         continue;

//       String qtyRaw = row.getFQuantity1();
//       BigDecimal qty;
//       try {
//         qty = new BigDecimal(qtyRaw.trim());
//       } catch (Exception e) {
//         errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
//         continue;
//       }
//       if (qty.compareTo(BigDecimal.ZERO) <= 0) {
//         errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
//         continue;
//       }

//       BigDecimal domeAmt = domeAmtByRow.get(rowNo);
//       if (domeAmt == null) {
//         errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
//         continue;
//       }

//       BigDecimal checkAmount;
//       try {
//         checkAmount = domeAmt.divide(qty, 6, RoundingMode.HALF_UP).stripTrailingZeros();
//       } catch (Exception e) {
//         errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
//         continue;
//       }

//       // summary1～6 すべて任意：空は null（＝条件未指定）
//       String s1 = nullIfBlank(row.getFSummary1());
//       String s2 = nullIfBlank(row.getFSummary2());
//       String s3 = nullIfBlank(row.getFSummary3());
//       String s4 = nullIfBlank(row.getFSummary4());
//       String s5 = nullIfBlank(row.getFSummary5());
//       String s6 = nullIfBlank(row.getFSummary6());

//       String accountCode = nullIfBlank(row.getFAccountCode());
//       String enterExpenses = nullIfBlank(row.getFEnterExpenses());

//       if (accountCode == null)
//         continue;

//       BigDecimal max = tblSumRepository.findMaxAmountWithFilters(
//           caseType, category1, category2Param,
//           s1, s2, s3, s4, s5, s6,
//           accountCode, enterExpenses,
//           txDate, needCategory2);

//       BigDecimal min = tblSumRepository.findMinAmountWithFilters(
//           caseType, category1, category2Param,
//           s1, s2, s3, s4, s5, s6,
//           accountCode, enterExpenses,
//           txDate, needCategory2);

//       if (min == null || max == null) {
//         errors.add("Row " + rowNo + ": 金額範囲が特定できません(摘要内容および交際費区分をご確認ください)");
//         continue;
//       }

//       if (checkAmount.compareTo(min) < 0 || checkAmount.compareTo(max) > 0) {
//         errors.add(
//             "Row " + rowNo + ": 国内通貨額/数量 = " + disp(checkAmount)
//                 + " is out of allowed range (" + disp(min) + " - " + disp(max) + ").");
//       }
//     }

//     if (CollectionUtils.isEmpty(meisai)) {
//       errors.add("明細なし");
//     }

//     if (!errors.isEmpty()) {
//       return CheckResponse.fromCheckResult(true, String.join("\n", errors));
//     }

//     return CheckResponse.fromCheckResult(false, "エラーなし");
//   }

//   private BigDecimal calcDomesticNetTaxExcludedByJar(BigDecimal docCurrAmt, BigDecimal rate) {

//     DomesticAmountRequest request = new DomesticAmountRequest();
//     DomesticAmountRequest.RequestBody requestBody = new DomesticAmountRequest.RequestBody();

//     FormModels.Form form1 = new FormModels.Form();

//     MeisaiItem meisaiItem = new MeisaiItem();
//     meisaiItem.setDocCurrAmt(docCurrAmt);

//     List<MeisaiItem> meisaiList = new ArrayList<>();
//     meisaiList.add(meisaiItem);

//     form1.setMeisai(meisaiList);
//     form1.setRate(rate);

//     requestBody.setForm(form1);
//     request.setRequestBody(requestBody);

//     DomesticAmountResponse resp = service.calculate(request);

//     if (resp == null
//         || resp.getResponseBody() == null
//         || resp.getResponseBody().getValue() == null) {
//       throw new IllegalStateException("国内通貨額計算の戻り値が不正です。");
//     }

//     List<CodeItem> codes = resp.getResponseBody().getValue().getCodes();
//     if (ObjectUtils.isEmpty(codes) || codes.get(0) == null || isBlank(codes.get(0).getKey())) {
//       throw new IllegalStateException("国内通貨額計算の戻り値 が空です。");
//     }

//     return new BigDecimal(codes.get(0).getKey().trim());
//   }

//   private static LocalDate resolveTransactionDate(AccountCodeRequest.CaseDto c) {
//     if (c == null || c.getExtensions() == null)
//       return null;

//     Object ext = c.getExtensions();
//     try {
//       Method m = ext.getClass().getMethod("getTransactionDate");
//       Object v = m.invoke(ext);

//       if (v == null)
//         return null;
//       if (v instanceof LocalDate ld)
//         return ld;

//       if (v instanceof String s) {
//         String raw = s.trim();
//         if (raw.isEmpty())
//           return null;
//         return DateParsers.parseFlexible(raw);
//       }
//       return null;
//     } catch (Exception e) {
//       return null;
//     }
//   }

//   private static void addHeaderTextErrors(List<String> errors, String value, String label) {
//     if (isBlank(value))
//       return;
//     if (containsWideChar(value))
//       errors.add(label + " に全角文字が含まれています。");
//     if (containsForbiddenChar(value))
//       errors.add(label + " に禁則文字が含まれています。");
//   }

//   private static boolean containsWideChar(String s) {
//     return s != null && s.codePoints().anyMatch(cp -> cp > 0x7E);
//   }

//   private static boolean containsForbiddenChar(String s) {
//     if (s == null)
//       return false;
//     final int[] forbidden = new int[] { '"', '\'', '\\', '/', '<', '>', '|', ':', '*', '?', '、' };
//     return s.codePoints().anyMatch(cp -> {
//       for (int f : forbidden)
//         if (cp == f)
//           return true;
//       return false;
//     });
//   }

//   @SuppressWarnings("unused")
//   private static boolean isAmountInputStarted(AccountCodeRequest.MeisaiRowDto row) {
//     if (row == null)
//       return false;
//     return !isBlank(row.getFQuantity1())
//         || !isBlank(row.getFDocCurrAmt())
//         || !isBlank(row.getFDomeCurrAmt());
//   }

//   private static boolean isHalfWidthInteger(String s) {
//     if (s == null)
//       return false;
//     String t = s.trim();
//     return !t.isEmpty() && t.matches("^[0-9]+$");
//   }

//   private static boolean isBlank(String s) {
//     return s == null || s.trim().isEmpty();
//   }

//   private static String nullIfBlank(String s) {
//     if (s == null)
//       return null;
//     String t = s.trim();
//     return t.isEmpty() ? null : t;
//   }

//   private static String trimToNull(String s) {
//     return nullIfBlank(s);
//   }

//   private static String disp(BigDecimal v) {
//     if (v == null)
//       return "";
//     return v.stripTrailingZeros().toPlainString();
//   }
// }

// // package com.jt.summary.service.impl;

// import com.jt.domesticamount.dto.DomesticAmountRequest;
// import com.jt.domesticamount.dto.DomesticAmountResponse;
// import com.jt.domesticamount.dto.DomesticAmountResponse.CodeItem;
// import com.jt.domesticamount.dto.FormModels;
// import com.jt.domesticamount.dto.FormModels.MeisaiItem;
// import com.jt.domesticamount.service.DomesticAmountService;
// import com.jt.summary.dto.AccountCodeRequest;
// import com.jt.summary.dto.CheckResponse;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.service.DomeCurrAmtOthService;
// import com.jt.summary.util.DateParsers;
// import java.lang.reflect.Method;
// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.LocalDate;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.util.CollectionUtils;
// import org.springframework.util.ObjectUtils;

// @Service
// @RequiredArgsConstructor
// public class DomeCurrAmtOthImp implements DomeCurrAmtOthService {

// // category1 が「海外 / Overseas / 収入金計上」の場合、category2 は “空扱い”
// // category1 が上記以外（＝国内想定）の場合、category2 は必須
// private static final Set<String> OVERSEAS_CATEGORIES = Set.of("海外",
// "Overseas", "収入金計上");

// private final DomesticAmountService service; // jar：国内通貨額(税抜)算出
// private final TblSumRepository tblSumRepository;

// @Override
// public CheckResponse checkDomesticUnitPrice(AccountCodeRequest req) {

// List<String> errors = new ArrayList<>();

// AccountCodeRequest.RequestBody body = (req != null) ? req.getRequestBody() :
// null;
// AccountCodeRequest.FormDto form = (body != null) ? body.getForm() : null;
// AccountCodeRequest.CaseDto c = (body != null) ? body.getCaseDto() : null;

// if (body == null)
// return CheckResponse.fromCheckResult(true, "requestBody が存在しません。");
// if (form == null)
// return CheckResponse.fromCheckResult(true, "requestBody.Form が存在しません。");
// if (c == null)
// return CheckResponse.fromCheckResult(true, "requestBody.case が存在しません。");

// List<AccountCodeRequest.MeisaiRowDto> meisai = (form.getMeisai() != null) ?
// form.getMeisai() : new ArrayList<>();

// // ===== header（master検索に追加する条件）=====
// String caseType = trimToNull(c.getCaseType());
// String category1 = (c.getCategoryLevel1() != null) ?
// trimToNull(c.getCategoryLevel1().getName()) : null;
// String category2 = (c.getCategoryLevel2() != null) ?
// trimToNull(c.getCategoryLevel2().getName()) : null;

// LocalDate txDate = resolveTransactionDate(c);

// boolean needCategory2 = (category1 != null) &&
// !OVERSEAS_CATEGORIES.contains(category1);
// String category2Param = needCategory2 ? trimToNull(category2) : null; //
// 海外/収入金は null

// // =========================================================
// // ★MOD: category2 必須ルール（最小追加）
// // =========================================================
// if (needCategory2 && category2Param == null) {
// return CheckResponse.fromCheckResult(
// true,
// "category1 が「" + (category1 == null ? "" : category1) + "」の場合、category2
// は必須です。");
// }

// // =========================================================
// // ★MOD: CHECK A/B：ヘッダチェック（fail-fast）
// // =========================================================
// List<String> headerErrors = new ArrayList<>();

// addHeaderTextErrors(headerErrors, form.getFSettlement1(), "決済番号1"); // ★MOD
// addHeaderTextErrors(headerErrors, form.getFSettlement2(), "決済番号2"); // ★MOD
// addHeaderTextErrors(headerErrors, form.getFSettlement3(), "決済番号3"); // ★MOD
// addHeaderTextErrors(headerErrors, form.getFInvoiceNumber(), "請求書番号"); // ★MOD

// // 合計金額：空OK、半角整数のみ
// String totalAmt = form.getFTotalAmountC(); // ★MOD
// if (!isBlank(totalAmt) && !isHalfWidthInteger(totalAmt)) { // ★MOD
// headerErrors.add("合計金額 は半角整数で入力してください。"); // ★MOD
// }

// errors.addAll(headerErrors);

// // =========================================================
// // 数値エラー行は後続チェックをスキップ
// // =========================================================
// Set<Integer> rowsWithNumericError = new HashSet<>();
// Map<Integer, BigDecimal> domeAmtByRow = new HashMap<>();

// BigDecimal rate = form.getFRate(); // jar算出が必要な時だけ使う

// int rowNo = 0;
// for (AccountCodeRequest.MeisaiRowDto row : meisai) {
// rowNo++;

// //保存前の全量チェック対応
// if (row == null) {
// errors.add("Row " + rowNo + ": 数量 は必須項目です。");
// errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
// //errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// continue;
// }

// String qtyRaw = row.getFQuantity1();
// String docAmtRaw = row.getFDocCurrAmt();
// String domeAmtRaw = row.getFDomeCurrAmt();

// // 数量：必須 / 半角整数 / >=1
// if (isBlank(qtyRaw)) {
// errors.add("Row " + rowNo + ": 数量 は必須項目です。");
// rowsWithNumericError.add(rowNo);
// } else if (!isHalfWidthInteger(qtyRaw)) {
// errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// BigDecimal qty = new BigDecimal(qtyRaw.trim());
// if (qty.compareTo(BigDecimal.ONE) < 0) {
// errors.add("Row " + rowNo + ": 数量 は1以上で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }
// // 伝票通貨額：必須 / 半角整数（チェックリスト通り）
// BigDecimal docAmt = null;
// if (isBlank(docAmtRaw)) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
// rowsWithNumericError.add(rowNo);
// } else if (!isHalfWidthInteger(docAmtRaw)) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// docAmt = new BigDecimal(docAmtRaw.trim());
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }
// // 国内通貨額(税抜)：画面値優先、なければ jar 算出（rate 必須）
// if (!rowsWithNumericError.contains(rowNo)) {

// BigDecimal domeAmt = null;

// //下拉未选中(空)时，先出「半角整数」メッセージに統一し、jar算出に入らないようにする
// if (isBlank(domeAmtRaw)) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
// } else {
// if (!isHalfWidthInteger(domeAmtRaw)) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// domeAmt = new BigDecimal(domeAmtRaw.trim());
// domeAmtByRow.put(rowNo, domeAmt);
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }
// }

// if (!rowsWithNumericError.contains(rowNo) && domeAmt == null) {
// // jar算出
// if (rate == null) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（レート が未設定）。");
// rowsWithNumericError.add(rowNo);
// } else if (docAmt == null) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) を算出できません（伝票通貨額 が不正）。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// BigDecimal calculated = calcDomesticNetTaxExcludedByJar(docAmt, rate);
// domeAmtByRow.put(rowNo, calculated);
// } catch (Exception ex) {
// errors.add("Row " + rowNo + ": 国内通貨額(税抜) の算出に失敗しました。（" + ex.getMessage() +
// "）");
// rowsWithNumericError.add(rowNo);
// }
// }
// }
// }

// String ac = nullIfBlank(row.getFAccountCode()); // ★MOD
// if (ac == null) { // ★MOD
// errors.add("Row " + rowNo + ": 勘定コードは必須です。"); // ★MOD
// }
// }
// // =========================================================
// // 金額範囲チェック（国内通貨額/数量）
// // =========================================================
// rowNo = 0;
// for (AccountCodeRequest.MeisaiRowDto row : meisai) {
// rowNo++;

// if (row == null)
// continue; // null 行は上でエラー済み

// // if (rowsWithNumericError.contains(rowNo))
// // continue;

// String qtyRaw = row.getFQuantity1();
// BigDecimal qty;
// try {
// qty = new BigDecimal(qtyRaw.trim());
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
// continue;
// }
// if (qty.compareTo(BigDecimal.ZERO) <= 0) {
// errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
// continue;
// }

// BigDecimal domeAmt = domeAmtByRow.get(rowNo);
// if (domeAmt == null) {
// errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
// continue;
// }

// BigDecimal checkAmount;
// try {
// checkAmount = domeAmt.divide(qty, 6,
// RoundingMode.HALF_UP).stripTrailingZeros();
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 国内通貨額／数量には正しい数値を入力してください。");
// continue;
// }

// String s1 = nullIfBlank(row.getFSummary1());
// String s2 = nullIfBlank(row.getFSummary2());
// String s3 = nullIfBlank(row.getFSummary3());
// String s4 = nullIfBlank(row.getFSummary4());
// String s5 = nullIfBlank(row.getFSummary5());
// String s6 = nullIfBlank(row.getFSummary6());

// String accountCode = nullIfBlank(row.getFAccountCode());
// String enterExpenses = nullIfBlank(row.getFEnterExpenses());

// if (accountCode == null)
// continue;

// BigDecimal max = tblSumRepository.findMaxAmountWithFilters(
// caseType, category1, category2Param,
// s1, s2, s3, s4, s5, s6,
// accountCode, enterExpenses,
// txDate, needCategory2);

// BigDecimal min = tblSumRepository.findMinAmountWithFilters(
// caseType, category1, category2Param,
// s1, s2, s3, s4, s5, s6,
// accountCode, enterExpenses,
// txDate, needCategory2);

// if (min == null || max == null) {
// errors.add("Row " + rowNo + ": 金額範囲が特定できません(摘要内容および交際費区分をご確認ください)");
// continue;
// }

// if (checkAmount.compareTo(min) < 0 || checkAmount.compareTo(max) > 0) {
// errors.add(
// "Row " + rowNo + ": 国内通貨額/数量 = " + disp(checkAmount)
// + " is out of allowed range (" + disp(min) + " - " + disp(max) + ").");
// }
// }
// if (CollectionUtils.isEmpty(meisai)) {
// errors.add("明細なし");
// }

// if (!errors.isEmpty()) {
// return CheckResponse.fromCheckResult(true, String.join("\n", errors));
// }

// return CheckResponse.fromCheckResult(false, "エラーなし");
// }

// private BigDecimal calcDomesticNetTaxExcludedByJar(BigDecimal docCurrAmt,
// BigDecimal rate) {

// DomesticAmountRequest request = new DomesticAmountRequest();
// DomesticAmountRequest.RequestBody requestBody = new
// DomesticAmountRequest.RequestBody();

// FormModels.Form form1 = new FormModels.Form();

// MeisaiItem meisaiItem = new MeisaiItem();
// meisaiItem.setDocCurrAmt(docCurrAmt);

// List<MeisaiItem> meisaiList = new ArrayList<>();
// meisaiList.add(meisaiItem);

// form1.setMeisai(meisaiList);
// form1.setRate(rate);

// requestBody.setForm(form1);
// request.setRequestBody(requestBody);

// DomesticAmountResponse resp = service.calculate(request);

// if (resp == null
// || resp.getResponseBody() == null
// || resp.getResponseBody().getValue() == null) {
// throw new IllegalStateException("国内通貨額計算の戻り値が不正です。");
// }

// List<CodeItem> codes = resp.getResponseBody().getValue().getCodes();
// if (ObjectUtils.isEmpty(codes) || codes.get(0) == null ||
// isBlank(codes.get(0).getKey())) {
// throw new IllegalStateException("国内通貨額計算の戻り値 が空です。");
// }

// return new BigDecimal(codes.get(0).getKey().trim());
// }

// private static LocalDate resolveTransactionDate(AccountCodeRequest.CaseDto c)
// {
// if (c == null || c.getExtensions() == null)
// return null;

// Object ext = c.getExtensions();
// try {
// Method m = ext.getClass().getMethod("getTransactionDate");
// Object v = m.invoke(ext);

// if (v == null)
// return null;
// if (v instanceof LocalDate ld)
// return ld;

// if (v instanceof String s) {
// String raw = s.trim();
// if (raw.isEmpty())
// return null;
// return DateParsers.parseFlexible(raw);
// }
// return null;
// } catch (Exception e) {
// return null;
// }
// }

// private static void addHeaderTextErrors(List<String> errors, String value,
// String label) {
// if (isBlank(value))
// return;
// if (containsWideChar(value))
// errors.add(label + " に全角文字が含まれています。");
// if (containsForbiddenChar(value))
// errors.add(label + " に禁則文字が含まれています。");
// }

// private static boolean containsWideChar(String s) {
// return s != null && s.codePoints().anyMatch(cp -> cp > 0x7E);
// }

// private static boolean containsForbiddenChar(String s) {
// if (s == null)
// return false;
// final int[] forbidden = new int[] { '"', '\'', '\\', '/', '<', '>', '|', ':',
// '*', '?', '、' };
// return s.codePoints().anyMatch(cp -> {
// for (int f : forbidden)
// if (cp == f)
// return true;
// return false;
// });
// }
// @SuppressWarnings("unused")
// private static boolean isAmountInputStarted(AccountCodeRequest.MeisaiRowDto
// row) {
// if (row == null)
// return false;
// return !isBlank(row.getFQuantity1())
// || !isBlank(row.getFDocCurrAmt())
// || !isBlank(row.getFDomeCurrAmt());
// }

// private static boolean isHalfWidthInteger(String s) {
// if (s == null)
// return false;
// String t = s.trim();
// return !t.isEmpty() && t.matches("^[0-9]+$");
// }

// private static boolean isBlank(String s) {
// return s == null || s.trim().isEmpty();
// }

// private static String nullIfBlank(String s) {
// if (s == null)
// return null;
// String t = s.trim();
// return t.isEmpty() ? null : t;
// }

// private static String trimToNull(String s) {
// return nullIfBlank(s);
// }

// private static String disp(BigDecimal v) {
// if (v == null)
// return "";
// return v.stripTrailingZeros().toPlainString();
// }
// }
