package com.jt.summary.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.CheckResponse;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.DocCurrAmtService;
import com.jt.summary.util.DateParsers;
import com.jt.taxamount.model.ApiResponse;
import com.jt.taxamount.service.TaxAmountService;
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

@Service
@RequiredArgsConstructor
public class DocCurrAmtImp implements DocCurrAmtService {

  private final TblSumRepository tblSumRepository;
  private final TaxAmountService taxAmountService; // jar：税額算出
  private final ObjectMapper objectMapper;

  // ✅ NEW: use category1_displayid rules from application.yml via
  // Category2RuleService
  private final Category2RuleService category2RuleService;

  @Override
  public CheckResponse checkJpyPrice(AccountCodeRequest req) {

    AccountCodeRequest.RequestBody body = (req != null) ? req.getRequestBody() : null;
    AccountCodeRequest.FormDto form = (body != null) ? body.getForm() : null;
    AccountCodeRequest.CaseDto c = (body != null) ? body.getCaseDto() : null;

    if (body == null)
      return fail("requestBody が存在しません。");
    if (form == null)
      return fail("requestBody.Form が存在しません。");
    if (c == null)
      return fail("requestBody.case が存在しません。");

    List<AccountCodeRequest.MeisaiRowDto> meisai = (form.getMeisai() != null) ? form.getMeisai() : new ArrayList<>();

    List<String> errors = new ArrayList<>();

    // =========================================================
    // CHECK A：ヘッダ文字チェック（全角・禁則）
    // =========================================================
    List<String> headerErrors = new ArrayList<>();

    addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement1"), "決済番号1");
    addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement2"), "決済番号2");
    addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement3"), "決済番号3");

    addHeaderTextErrors(
        headerErrors,
        getOptionalString(form, "getFInvoiceNumber", "getFInvoice_number", "getFInvoiceNo", "getFInvoice_no"),
        "請求書番号");

    String totalAmt = getOptionalString(form, "getFTotalAmountC", "getFTotalAmountC", "getF_Total_Amount_C");
    if (!isBlank(totalAmt) && !isHalfWidthInteger(totalAmt)) {
      headerErrors.add("合計金額 は半角整数で入力してください。");
    }
    errors.addAll(headerErrors);

    // =========================================================
    // header（master検索に追加する条件）
    // caseType / category1_displayid / (need? category2_displayid) /
    // TransactionDate
    // =========================================================
    String caseType = trimToNull(c.getCaseType());

    // ✅ MOD: use displayId (NOT name)
    String category1DisplayId = (c.getCategoryLevel1() != null)
        ? trimToNull(c.getCategoryLevel1().getDisplayId())
        : null;
    String category2DisplayId = (c.getCategoryLevel2() != null)
        ? trimToNull(c.getCategoryLevel2().getDisplayId())
        : null;

    LocalDate txDate = resolveTransactionDate(c);

    // ✅ MOD: determine if category2 is required by caseType + category1_displayid
    boolean needCategory2 = category2RuleService.needCategory2(caseType, category1DisplayId);

    String category2Param = needCategory2 ? category2DisplayId : null;

    boolean skipRangeCheck = false;
    if (needCategory2 && category2Param == null) {
      errors.add(
          "category1_displayid が「"
              + (category1DisplayId == null ? "" : category1DisplayId)
              + "」の場合、category2_displayid は必須です。");
      skipRangeCheck = true;
    }

    // =========================================================
    // 保存前全量チェック（明細）
    // - 数値エラー行は後続の範囲チェックをスキップ
    // =========================================================
    Set<Integer> rowsWithNumericError = new HashSet<>();
    Map<Integer, BigDecimal> checkAmountByRow = new HashMap<>();
    Map<Integer, Boolean> taxAmountReadyByRow = new HashMap<>();

    // =========================================================
    // CHECK B：数量「伝票通貨額」税額（同一行で全エラーを出す）
    // =========================================================
    int rowNo = 0;
    for (AccountCodeRequest.MeisaiRowDto row : meisai) {
      rowNo++;

      if (row == null) {
        errors.add("Row " + rowNo + ": 数量 は必須項目です。");
        errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
        errors.add("Row " + rowNo + ": 勘定コードは必須です。");
        errors.add("Row " + rowNo + ": 税コードは必須です。");

        rowsWithNumericError.add(rowNo);
        taxAmountReadyByRow.put(rowNo, false);
        continue;
      }

      String qtyRaw = row.getFQuantity1();
      String docAmtRaw = row.getFDocCurrAmt();

      BigDecimal qtyVal = null;
      BigDecimal docValTaxIncluded = null;

      // 数量：必須 / 半角整数 / >=1
      if (isBlank(qtyRaw)) {
        errors.add("Row " + rowNo + ": 数量 は必須項目です。");
        rowsWithNumericError.add(rowNo);
      } else if (!isHalfWidthInteger(qtyRaw)) {
        errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
        rowsWithNumericError.add(rowNo);
      } else {
        try {
          qtyVal = new BigDecimal(qtyRaw.trim());
          if (qtyVal.compareTo(BigDecimal.ONE) < 0) {
            errors.add("Row " + rowNo + ": 数量 は1以上で入力してください。");
            rowsWithNumericError.add(rowNo);
          }
        } catch (Exception e) {
          errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
          rowsWithNumericError.add(rowNo);
        }
      }

      // 伝票通貨額：必須 / 半角整数
      if (isBlank(docAmtRaw)) {
        errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
        rowsWithNumericError.add(rowNo);
      } else if (!isHalfWidthInteger(docAmtRaw)) {
        errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
        rowsWithNumericError.add(rowNo);
      } else {
        try {
          docValTaxIncluded = new BigDecimal(docAmtRaw.trim());
        } catch (Exception e) {
          errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
          rowsWithNumericError.add(rowNo);
        }
      }

      // 勘定コード：必須
      String accountCode = nullIfBlank(row.getFAccountCode());
      if (accountCode == null) {
        errors.add("Row " + rowNo + ": 勘定コードは必須です。");
      }

      // 税コード：必須
      String taxCode = trimToNull(getOptionalString(row, "getFTaxCode", "getFTax_code"));
      if (taxCode == null) {
        errors.add("Row " + rowNo + ": 税コードは必須です。");
        rowsWithNumericError.add(rowNo);
      }

      String taxAmountRaw = getOptionalString(row, "getFTaxAmount", "getFTax_amount");
      BigDecimal taxVal = null;
      boolean taxReady = false;

      boolean taxAmountUnselected = isTaxAmountUnselected(taxAmountRaw);

      if (!taxAmountUnselected) {
        if (!isHalfWidthInteger(taxAmountRaw)) {
          errors.add("Row " + rowNo + ": 税額 は半角整数で入力してください。");
          rowsWithNumericError.add(rowNo);
        } else {
          try {
            taxVal = new BigDecimal(taxAmountRaw.trim());
            taxReady = true;
          } catch (Exception e) {
            errors.add("Row " + rowNo + ": 税額 は半角整数で入力してください。");
            rowsWithNumericError.add(rowNo);
          }
        }
      } else {
        if (taxCode != null && docValTaxIncluded != null) {
          BigDecimal jarTax = calcTaxAmountByJar(docValTaxIncluded, taxCode, caseType);
          if (jarTax != null) {
            jarTax = jarTax.setScale(0, RoundingMode.DOWN);
            taxVal = jarTax;
            taxReady = true;
          } else {
            errors.add("Row " + rowNo + ": 税額 は必須です。");
            rowsWithNumericError.add(rowNo);
          }
        } else {
          errors.add("Row " + rowNo + ": 税額 は必須です。");
          rowsWithNumericError.add(rowNo);
        }
      }

      taxAmountReadyByRow.put(rowNo, taxReady);

      if (rowsWithNumericError.contains(rowNo)) {
        continue;
      }

      if (qtyVal == null || docValTaxIncluded == null || taxVal == null) {
        errors.add("Row " + rowNo + ": 伝票通貨額／数量／税額 に不正な数値が入力されています。");
        rowsWithNumericError.add(rowNo);
        taxAmountReadyByRow.put(rowNo, false);
        continue;
      }

      try {
        BigDecimal checkAmount = docValTaxIncluded
            .subtract(taxVal)
            .divide(qtyVal, 6, RoundingMode.HALF_UP)
            .stripTrailingZeros();

        checkAmountByRow.put(rowNo, checkAmount);
      } catch (Exception e) {
        errors.add("Row " + rowNo + ": 伝票通貨額／数量／税額 に不正な数値が入力されています。");
        rowsWithNumericError.add(rowNo);
        taxAmountReadyByRow.put(rowNo, false);
      }
    }

    // =========================================================
    // CHECK D：金額範囲（MIN/MAX）検索
    // =========================================================
    rowNo = 0;
    for (AccountCodeRequest.MeisaiRowDto row : meisai) {
      rowNo++;
      if (row == null || !isUserStartedRow(row))
        continue;

      if (rowsWithNumericError.contains(rowNo))
        continue;

      if (!taxAmountReadyByRow.getOrDefault(rowNo, false)) {
        continue;
      }

      BigDecimal checkAmount = checkAmountByRow.get(rowNo);
      if (checkAmount == null)
        continue;

      String accountCode = nullIfBlank(row.getFAccountCode());
      if (accountCode == null)
        continue;

      if (skipRangeCheck)
        continue;

      String enterExpenses = nullIfBlank(row.getFEnterExpenses());

      // SUMMARY1..6（summary1 は任意）
      String s1 = nullIfBlank(row.getFSummary1());
      String s2 = nullIfBlank(row.getFSummary2());
      String s3 = nullIfBlank(row.getFSummary3());
      String s4 = nullIfBlank(row.getFSummary4());
      String s5 = nullIfBlank(row.getFSummary5());
      String s6 = nullIfBlank(row.getFSummary6());

      // summary1 が未指定なら、下位 summary も条件に使わない（不整合防止）
      if (s1 == null) {
        s2 = null;
        s3 = null;
        s4 = null;
        s5 = null;
        s6 = null;
      }

      BigDecimal max = tblSumRepository.findMaxAmountWithFilters(
          caseType,
          category1DisplayId,
          category2Param,
          s1,
          s2,
          s3,
          s4,
          s5,
          s6,
          accountCode,
          enterExpenses,
          txDate,
          needCategory2);

      BigDecimal min = tblSumRepository.findMinAmountWithFilters(
          caseType,
          category1DisplayId,
          category2Param,
          s1,
          s2,
          s3,
          s4,
          s5,
          s6,
          accountCode,
          enterExpenses,
          txDate,
          needCategory2);

      if (min == null || max == null) {
        errors.add("Row " + rowNo + ": 金額範囲が特定できません(摘要内容および交際費区分をご確認ください)");
        continue;
      }

      if (checkAmount.compareTo(min) < 0 || checkAmount.compareTo(max) > 0) {
        errors.add(
            "Row "
                + rowNo
                + ": (伝票通貨額(税込) - 税額) / 数量 = "
                + disp(checkAmount)
                + " is out of allowed range ("
                + disp(min)
                + " - "
                + disp(max)
                + ").");
      }
    }

    if (CollectionUtils.isEmpty(meisai)) {
      errors.add("明細なし");
    }

    if (!errors.isEmpty()) {
      return fail(String.join("\n", errors));
    }

    return CheckResponse.fromCheckResult(false, "エラーなし");
  }

  // =========================================================
  // 税額が「未選択」の値をページ差異込みで吸収する
  // =========================================================
  private static boolean isTaxAmountUnselected(String raw) {
    if (raw == null)
      return true;
    String t = raw.trim();
    if (t.isEmpty())
      return true;

    if ("null".equalsIgnoreCase(t))
      return true;
    if ("undefined".equalsIgnoreCase(t))
      return true;
    if ("未選択".equals(t))
      return true;
    if ("選択してください".equals(t))
      return true;

    if ("0".equals(t) || "1".equals(t))
      return true;

    return false;
  }

  // =========================================================
  // taxamount.jar 呼び出し → resp.responseBody.value.codes[0].key を税額として取得
  // =========================================================
  private BigDecimal calcTaxAmountByJar(
      BigDecimal docCurrAmtTaxIncluded, String taxCode, String caseType) {

    try {
      Map<String, Object> item = new HashMap<>();
      item.put("F_Tax_code", taxCode);
      item.put("F_Doc_curr_Amt", docCurrAmtTaxIncluded.toPlainString());

      Map<String, Object> form = new HashMap<>();
      form.put("F_meisai", List.of(item));

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("Form", form);

      if (!isBlank(caseType)) {
        Map<String, Object> caseMap = new HashMap<>();
        caseMap.put("caseType", caseType.trim());
        requestBody.put("case", caseMap);
      }

      Map<String, Object> root = new HashMap<>();
      root.put("requestBody", requestBody);

      JsonNode payload = objectMapper.valueToTree(root);

      ApiResponse resp = taxAmountService.calculate(payload);

      if (resp == null || resp.responseBody == null || resp.responseBody.value == null) {
        return null;
      }

      List<ApiResponse.CodeItem> codes = resp.responseBody.value.codes;
      if (codes == null || codes.isEmpty() || codes.get(0) == null || isBlank(codes.get(0).key)) {
        return null;
      }

      String raw = codes.get(0).key.trim().replace(",", "");
      if (raw.isEmpty())
        return null;

      return new BigDecimal(raw);
    } catch (Exception e) {
      return null;
    }
  }

  // =========================================================
  // TransactionDate：Extensions.getTransactionDate() が String/LocalDate どちらでも対応
  // =========================================================
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
    return s.codePoints()
        .anyMatch(
            cp -> {
              for (int f : forbidden)
                if (cp == f)
                  return true;
              return false;
            });
  }

  private static boolean isUserStartedRow(AccountCodeRequest.MeisaiRowDto row) {
    if (row == null) {
      return false;
    }

    String taxAmount = getOptionalString(row, "getFTaxAmount", "getFTax_amount");
    String taxCode = getOptionalString(row, "getFTaxCode", "getFTax_code");
    String domeCurrAmt = getOptionalString(row, "getFDomeCurrAmt", "getF_Dome_Curr_Amt");

    return !isBlank(row.getFQuantity1())
        || !isBlank(row.getFDocCurrAmt())
        || !isBlank(domeCurrAmt)
        || !isBlank(row.getFSummary1())
        || !isBlank(row.getFSummary2())
        || !isBlank(row.getFSummary3())
        || !isBlank(row.getFSummary4())
        || !isBlank(row.getFSummary5())
        || !isBlank(row.getFSummary6())
        || !isBlank(row.getFAccountCode())
        || !isBlank(row.getFEnterExpenses())
        || !isBlank(taxAmount)
        || !isBlank(taxCode);
  }

  private static boolean isHalfWidthInteger(String s) {
    if (s == null)
      return false;
    String t = s.trim();
    return !t.isEmpty() && t.matches("^[0-9]+$");
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

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String disp(BigDecimal v) {
    if (v == null)
      return "";
    return v.stripTrailingZeros().toPlainString();
  }

  private static String getOptionalString(Object target, String... methodNames) {
    if (target == null || methodNames == null)
      return null;
    for (String mn : methodNames) {
      try {
        Method m = target.getClass().getMethod(mn);
        Object v = m.invoke(target);
        if (v == null)
          continue;
        String s = String.valueOf(v);
        if (!s.trim().isEmpty())
          return s;
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  private static CheckResponse fail(String msg) {
    return CheckResponse.fromCheckResult(true, msg);
  }
}

// package com.jt.summary.service.impl;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.jt.summary.dto.AccountCodeRequest;
// import com.jt.summary.dto.CheckResponse;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.service.DocCurrAmtService;
// import com.jt.summary.util.DateParsers;
// import com.jt.taxamount.model.ApiResponse;
// import com.jt.taxamount.service.TaxAmountService;
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

// @Service
// @RequiredArgsConstructor
// public class DocCurrAmtImp implements DocCurrAmtService {

// // // 固定資産（Fixed Asset）系の caseType
// // private static final Set<String> CASETYPE_ASSET_CATE = Set.of("ZD11");

// // // Category Level1
// // private static final String CATEGORY1_DOMESTIC = "国内";
// // private static final String CATEGORY1_FOREIGN = "海外";
// // private static final String CATEGORY1_OVERSEAS = "Overseas";
// // private static final String CATEGORY1_INCOME = "収入金計上";

// // /**
// // * 海外/Overseas/収入金計上 は category2 を “空扱い”
// // * ※ただし 固定資産(ZD11) かつ category1=海外 の場合は category2 必須
// // */
// // private static final Set<String> OVERSEAS_CATEGORIES =
// Set.of(CATEGORY1_FOREIGN, CATEGORY1_OVERSEAS,
// // CATEGORY1_INCOME);

// //use category1_displayid rules from application.yml via Category2RuleService
// private final Category2RuleService category2RuleService;
// private final TblSumRepository tblSumRepository;
// private final TaxAmountService taxAmountService; // jar：税額算出
// private final ObjectMapper objectMapper;

// @Override
// public CheckResponse checkJpyPrice(AccountCodeRequest req) {

// AccountCodeRequest.RequestBody body = (req != null) ? req.getRequestBody() :
// null;
// AccountCodeRequest.FormDto form = (body != null) ? body.getForm() : null;
// AccountCodeRequest.CaseDto c = (body != null) ? body.getCaseDto() : null;

// if (body == null)
// return fail("requestBody が存在しません。");
// if (form == null)
// return fail("requestBody.Form が存在しません。");
// if (c == null)
// return fail("requestBody.case が存在しません。");

// List<AccountCodeRequest.MeisaiRowDto> meisai = (form.getMeisai() != null) ?
// form.getMeisai() : new ArrayList<>();

// List<String> errors = new ArrayList<>();

// // =========================================================
// // CHECK A：ヘッダ文字チェック（全角・禁則）
// // =========================================================
// List<String> headerErrors = new ArrayList<>();

// addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement1"),
// "決済番号1");
// addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement2"),
// "決済番号2");
// addHeaderTextErrors(headerErrors, getOptionalString(form, "getFSettlement3"),
// "決済番号3");

// addHeaderTextErrors(
// headerErrors,
// getOptionalString(form, "getFInvoiceNumber", "getFInvoice_number",
// "getFInvoiceNo", "getFInvoice_no"),
// "請求書番号");

// String totalAmt = getOptionalString(form, "getFTotalAmountC",
// "getFTotalAmountC", "getF_Total_Amount_C");
// if (!isBlank(totalAmt) && !isHalfWidthInteger(totalAmt)) {
// headerErrors.add("合計金額 は半角整数で入力してください。");
// }
// errors.addAll(headerErrors);

// // =========================================================
// // header（master検索に追加する条件）
// // caseType / category1 / (海外はcategory2空扱い) / TransactionDate
// // =========================================================
// String caseType = trimToNull(c.getCaseType());

// // normalizeCategory を使わない（UIの値をそのまま扱う）
// String category1 = (c.getCategoryLevel1() != null) ?
// trimToNull(c.getCategoryLevel1().getName()) : null;
// String category2 = (c.getCategoryLevel2() != null) ?
// trimToNull(c.getCategoryLevel2().getName()) : null;

// LocalDate txDate = resolveTransactionDate(c);

// boolean isAssetCaseType = (caseType != null) &&
// CASETYPE_ASSET_CATE.contains(caseType.trim());

// // 海外/Overseas/収入金計上は不要
// // 固定資産(ZD11)かつ category1=海外 のときだけ必須化する
// boolean needCategory2;
// if (CATEGORY1_DOMESTIC.equals(category1)) {
// needCategory2 = true;//（国内は必ず category2 必須）
// } else {
// needCategory2 = (category1 != null) &&
// !OVERSEAS_CATEGORIES.contains(category1);
// }

// if (isAssetCaseType && CATEGORY1_FOREIGN.equals(category1)) {
// needCategory2 = true; //（ZD11 かつ 海外は必須）
// }
// String category2Param = needCategory2 ? category2 : null;
// boolean skipRangeCheck = false; // ヘッダ条件不備なら範囲チェックをスキップ
// if (needCategory2 && category2Param == null) {
// errors.add("category1 が「" + (category1 == null ? "" : category1) +
// "」の場合、category2 は必須です。");
// skipRangeCheck = true;
// }

// // =========================================================
// // 保存前全量チェック（明細）
// // - 数値エラー行は後続の範囲チェックをスキップ
// // =========================================================
// Set<Integer> rowsWithNumericError = new HashSet<>();
// Map<Integer, BigDecimal> checkAmountByRow = new HashMap<>();

// Map<Integer, Boolean> taxAmountReadyByRow = new HashMap<>();

// // =========================================================
// // CHECK B：数量「伝票通貨額」税額（同一行で全エラーを出す）
// // =========================================================
// int rowNo = 0;
// for (AccountCodeRequest.MeisaiRowDto row : meisai) {
// rowNo++;

// if (row == null) {
// errors.add("Row " + rowNo + ": 数量 は必須項目です。");
// errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
// errors.add("Row " + rowNo + ": 勘定コードは必須です。");
// errors.add("Row " + rowNo + ": 税コードは必須です。");
// errors.add("Row " + rowNo + ": 税額 は必須です。");
// rowsWithNumericError.add(rowNo);
// taxAmountReadyByRow.put(rowNo, false);
// continue;
// }

// String qtyRaw = row.getFQuantity1();
// String docAmtRaw = row.getFDocCurrAmt();

// BigDecimal qtyVal = null;
// BigDecimal docValTaxIncluded = null;

// // 数量：必須 / 半角整数 / >=1
// if (isBlank(qtyRaw)) {
// errors.add("Row " + rowNo + ": 数量 は必須項目です。");
// rowsWithNumericError.add(rowNo);
// } else if (!isHalfWidthInteger(qtyRaw)) {
// errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// qtyVal = new BigDecimal(qtyRaw.trim());
// if (qtyVal.compareTo(BigDecimal.ONE) < 0) {
// errors.add("Row " + rowNo + ": 数量 は1以上で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 数量 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }

// // 伝票通貨額：必須 / 半角整数
// if (isBlank(docAmtRaw)) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は必須です。");
// rowsWithNumericError.add(rowNo);
// } else if (!isHalfWidthInteger(docAmtRaw)) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// docValTaxIncluded = new BigDecimal(docAmtRaw.trim());
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 伝票通貨額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }

// // 勘定コード：必須
// String accountCode = nullIfBlank(row.getFAccountCode());
// if (accountCode == null) {
// errors.add("Row " + rowNo + ": 勘定コードは必須です。");
// }

// // 税コード：必須
// String taxCode = trimToNull(getOptionalString(row, "getFTaxCode",
// "getFTax_code"));
// if (taxCode == null) {
// errors.add("Row " + rowNo + ": 税コードは必須です。");
// rowsWithNumericError.add(rowNo);
// }

// String taxAmountRaw = getOptionalString(row, "getFTaxAmount",
// "getFTax_amount");
// BigDecimal taxVal = null;
// boolean taxReady = false;

// boolean taxAmountUnselected = isTaxAmountUnselected(taxAmountRaw);

// if (!taxAmountUnselected) {
// if (!isHalfWidthInteger(taxAmountRaw)) {
// errors.add("Row " + rowNo + ": 税額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// } else {
// try {
// taxVal = new BigDecimal(taxAmountRaw.trim());
// taxReady = true;
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 税額 は半角整数で入力してください。");
// rowsWithNumericError.add(rowNo);
// }
// }
// } else {
// if (taxCode != null && docValTaxIncluded != null) {
// BigDecimal jarTax = calcTaxAmountByJar(docValTaxIncluded, taxCode, caseType);
// if (jarTax != null) {
// jarTax = jarTax.setScale(0, RoundingMode.DOWN);
// taxVal = jarTax;
// taxReady = true;
// } else {
// errors.add("Row " + rowNo + ": 税額 は必須です。");
// rowsWithNumericError.add(rowNo);
// }
// } else {
// errors.add("Row " + rowNo + ": 税額 は必須です。");
// rowsWithNumericError.add(rowNo);
// }
// }

// taxAmountReadyByRow.put(rowNo, taxReady);

// if (rowsWithNumericError.contains(rowNo)) {
// continue;
// }

// if (qtyVal == null || docValTaxIncluded == null || taxVal == null) {
// errors.add("Row " + rowNo + ": 伝票通貨額／数量／税額 に不正な数値が入力されています。");
// rowsWithNumericError.add(rowNo);
// taxAmountReadyByRow.put(rowNo, false);
// continue;
// }

// try {
// BigDecimal checkAmount = docValTaxIncluded
// .subtract(taxVal)
// .divide(qtyVal, 6, RoundingMode.HALF_UP)
// .stripTrailingZeros();

// checkAmountByRow.put(rowNo, checkAmount);
// } catch (Exception e) {
// errors.add("Row " + rowNo + ": 伝票通貨額／数量／税額 に不正な数値が入力されています。");
// rowsWithNumericError.add(rowNo);
// taxAmountReadyByRow.put(rowNo, false);
// }
// }

// // =========================================================
// // CHECK D：金額範囲（MIN/MAX）検索
// // =========================================================
// rowNo = 0;
// for (AccountCodeRequest.MeisaiRowDto row : meisai) {
// rowNo++;
// if (row == null || !isUserStartedRow(row))
// continue;

// if (rowsWithNumericError.contains(rowNo))
// continue;

// if (!taxAmountReadyByRow.getOrDefault(rowNo, false)) {
// continue;
// }

// BigDecimal checkAmount = checkAmountByRow.get(rowNo);
// if (checkAmount == null)
// continue;

// String accountCode = nullIfBlank(row.getFAccountCode());
// if (accountCode == null)
// continue;

// if (skipRangeCheck)
// continue;

// String enterExpenses = nullIfBlank(row.getFEnterExpenses());

// // SUMMARY1..6（summary1 は任意）
// String s1 = nullIfBlank(row.getFSummary1());
// String s2 = nullIfBlank(row.getFSummary2());
// String s3 = nullIfBlank(row.getFSummary3());
// String s4 = nullIfBlank(row.getFSummary4());
// String s5 = nullIfBlank(row.getFSummary5());
// String s6 = nullIfBlank(row.getFSummary6());

// // ✅ 最小変更：summary1 が未指定なら、下位 summary も条件に使わない（不整合防止）
// if (s1 == null) {
// s2 = null;
// s3 = null;
// s4 = null;
// s5 = null;
// s6 = null;
// }

// BigDecimal max = tblSumRepository.findMaxAmountWithFilters(
// caseType,
// category1,
// category2Param,
// s1,
// s2,
// s3,
// s4,
// s5,
// s6,
// accountCode,
// enterExpenses,
// txDate,
// needCategory2);

// BigDecimal min = tblSumRepository.findMinAmountWithFilters(
// caseType,
// category1,
// category2Param,
// s1,
// s2,
// s3,
// s4,
// s5,
// s6,
// accountCode,
// enterExpenses,
// txDate,
// needCategory2);

// if (min == null || max == null) {
// errors.add("Row " + rowNo + ": 金額範囲が特定できません(摘要内容および交際費区分をご確認ください)");
// continue;
// }

// if (checkAmount.compareTo(min) < 0 || checkAmount.compareTo(max) > 0) {
// errors.add(
// "Row "
// + rowNo
// + ": (伝票通貨額(税込) - 税額) / 数量 = "
// + disp(checkAmount)
// + " is out of allowed range ("
// + disp(min)
// + " - "
// + disp(max)
// + ").");
// }
// }

// if (CollectionUtils.isEmpty(meisai)) {
// errors.add("明細なし");
// }

// if (!errors.isEmpty()) {
// return fail(String.join("\n", errors));
// }

// return CheckResponse.fromCheckResult(false, "エラーなし");
// }

// // =========================================================
// // 税額が「未選択」の値をページ差異込みで吸収する
// // =========================================================
// private static boolean isTaxAmountUnselected(String raw) {
// if (raw == null)
// return true;
// String t = raw.trim();
// if (t.isEmpty())
// return true;

// if ("null".equalsIgnoreCase(t))
// return true;
// if ("undefined".equalsIgnoreCase(t))
// return true;
// if ("未選択".equals(t))
// return true;
// if ("選択してください".equals(t))
// return true;

// if ("0".equals(t) || "1".equals(t))
// return true;

// return false;
// }

// // =========================================================
// // taxamount.jar 呼び出し → resp.responseBody.value.codes[0].key を税額として取得
// // =========================================================
// private BigDecimal calcTaxAmountByJar(
// BigDecimal docCurrAmtTaxIncluded, String taxCode, String caseType) {

// try {
// Map<String, Object> item = new HashMap<>();
// item.put("F_Tax_code", taxCode);
// item.put("F_Doc_curr_Amt", docCurrAmtTaxIncluded.toPlainString());

// Map<String, Object> form = new HashMap<>();
// form.put("F_meisai", List.of(item));

// Map<String, Object> requestBody = new HashMap<>();
// requestBody.put("Form", form);

// if (!isBlank(caseType)) {
// Map<String, Object> caseMap = new HashMap<>();
// caseMap.put("caseType", caseType.trim());
// requestBody.put("case", caseMap);
// }

// Map<String, Object> root = new HashMap<>();
// root.put("requestBody", requestBody);

// JsonNode payload = objectMapper.valueToTree(root);

// ApiResponse resp = taxAmountService.calculate(payload);

// if (resp == null || resp.responseBody == null || resp.responseBody.value ==
// null) {
// return null;
// }

// List<ApiResponse.CodeItem> codes = resp.responseBody.value.codes;
// if (codes == null || codes.isEmpty() || codes.get(0) == null ||
// isBlank(codes.get(0).key)) {
// return null;
// }

// String raw = codes.get(0).key.trim().replace(",", "");
// if (raw.isEmpty())
// return null;

// return new BigDecimal(raw);
// } catch (Exception e) {
// return null;
// }
// }

// // =========================================================
// // TransactionDate：Extensions.getTransactionDate() が String/LocalDate どちらでも対応
// // =========================================================
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
// return s.codePoints()
// .anyMatch(
// cp -> {
// for (int f : forbidden)
// if (cp == f)
// return true;
// return false;
// });
// }

// private static boolean isUserStartedRow(AccountCodeRequest.MeisaiRowDto row)
// {
// if (row == null) {
// return false;
// }

// String taxAmount = getOptionalString(row, "getFTaxAmount", "getFTax_amount");
// String taxCode = getOptionalString(row, "getFTaxCode", "getFTax_code");
// String domeCurrAmt = getOptionalString(row, "getFDomeCurrAmt",
// "getF_Dome_Curr_Amt");

// return !isBlank(row.getFQuantity1())
// || !isBlank(row.getFDocCurrAmt())
// || !isBlank(domeCurrAmt)
// || !isBlank(row.getFSummary1())
// || !isBlank(row.getFSummary2())
// || !isBlank(row.getFSummary3())
// || !isBlank(row.getFSummary4())
// || !isBlank(row.getFSummary5())
// || !isBlank(row.getFSummary6())
// || !isBlank(row.getFAccountCode())
// || !isBlank(row.getFEnterExpenses())
// || !isBlank(taxAmount)
// || !isBlank(taxCode);
// }

// private static boolean isHalfWidthInteger(String s) {
// if (s == null)
// return false;
// String t = s.trim();
// return !t.isEmpty() && t.matches("^[0-9]+$");
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

// private static boolean isBlank(String s) {
// return s == null || s.trim().isEmpty();
// }

// private static String disp(BigDecimal v) {
// if (v == null)
// return "";
// return v.stripTrailingZeros().toPlainString();
// }

// private static String getOptionalString(Object target, String... methodNames)
// {
// if (target == null || methodNames == null)
// return null;
// for (String mn : methodNames) {
// try {
// Method m = target.getClass().getMethod(mn);
// Object v = m.invoke(target);
// if (v == null)
// continue;
// String s = String.valueOf(v);
// if (!s.trim().isEmpty())
// return s;
// } catch (Exception ignore) {
// }
// }
// return null;
// }

// private static CheckResponse fail(String msg) {
// return CheckResponse.fromCheckResult(true, msg);
// }
// }
