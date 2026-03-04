package com.jt.summary.service.impl;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.AccountCodeService;
import com.jt.summary.util.RequestContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountCodeServiceImpl implements AccountCodeService {

    private final TblSumRepository tblSumRepository;

    // use category1_displayid rules from application.yml
    private final Category2RuleService category2RuleService;

    @Override
    public SummaryResponse getAccountCode(AccountCodeRequest req) {

        RequestContext ctx = RequestContext.from(req);

        if (ctx.isMeisaiEmpty()) {
            return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
        }

        // input: case.caseType
        String caseType = trimToNull(ctx.getCaseType());

        // input: case.categoryLevel1.displayId  (category1_displayid)
        String category1DisplayId = trimToNull(ctx.getCategory1());

        // needCategory2 is decided by (caseType + category1_displayid)
        boolean needCategory2 = category2RuleService.needCategory2(caseType, category1DisplayId);

        // input: case.categoryLevel2.displayId (category2_displayid)
        String category2DisplayId = trimToNull(ctx.getCategory2());

        if (needCategory2 && category2DisplayId == null) {
            return SummaryResponse.error("category2_displayid is required for this category1_displayid");
        }

        // input: Form.F_meisai[0].F_summary1..6
        String s1 = trimToNull(ctx.getSummary(1));
        String s2 = trimToNull(ctx.getSummary(2));
        String s3 = trimToNull(ctx.getSummary(3));
        String s4 = trimToNull(ctx.getSummary(4));
        String s5 = trimToNull(ctx.getSummary(5));
        String s6 = trimToNull(ctx.getSummary(6));

        // 如果 summary1 没填，就不让 summary2..6 参与过滤（避免条件“断层”导致查不到）
        if (s1 == null) {
            s2 = null;
            s3 = null;
            s4 = null;
            s5 = null;
            s6 = null;
        }

        List<String> accountCodes = tblSumRepository.findDistinctAccountCodesWithFilters(
                caseType,
                category1DisplayId,
                needCategory2 ? category2DisplayId : null,
                emptyIfNull(s1),
                emptyIfNull(s2),
                emptyIfNull(s3),
                emptyIfNull(s4),
                emptyIfNull(s5),
                emptyIfNull(s6),
                ctx.getTransactionDate(),   // input: case.extensions.TransactionDate
                needCategory2
        );

        List<SummaryResponse.CodeDto> codes = (accountCodes == null)
                ? List.of()
                : accountCodes.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .distinct()
                    .map(v -> new SummaryResponse.CodeDto(v, ""))
                    .collect(Collectors.toList());

        return SummaryResponse.success(codes);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}


// package com.jt.summary.service.impl;

// import com.jt.summary.dto.AccountCodeRequest;
// import com.jt.summary.dto.SummaryResponse;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.service.AccountCodeService;
// import com.jt.summary.util.RequestContext;
// import java.util.List;
// import java.util.Set; 
// import java.util.stream.Collectors;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor
// public class AccountCodeServiceImpl implements AccountCodeService {

//     // 固定資産（Fixed Asset）系の caseType
//     private static final Set<String> CASETYPE_ASSET_CATE = Set.of(
//             "ZD11");

//     // Category Level1 
//     private static final String CATEGORY1_DOMESTIC = "国内";
//     private static final String CATEGORY1_FOREIGN = "海外";
//     private static final String CATEGORY1_OVERSEAS = "Overseas";
//     private static final String CATEGORY1_INCOME = "収入金計上";

//     private final TblSumRepository tblSumRepository;

//     @Override
//     public SummaryResponse getAccountCode(AccountCodeRequest req) {

//         RequestContext ctx = RequestContext.from(req);

//         if (ctx.isMeisaiEmpty()) {
//             return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
//         }

//         // needCategory2 逻辑调整（固定資産优先）
//         String caseType = nvl(ctx.getCaseType());
//         String category1 = nvl(ctx.getCategory1());

//         boolean isAssetCaseType = CASETYPE_ASSET_CATE.contains(caseType); 
//         boolean needCategory2;
//         if (isAssetCaseType) { // 固定資産なら国内/海外問わず必須
//             needCategory2 = true;
//         } else { // 非固定資産
//             switch (category1) {
//                 case CATEGORY1_DOMESTIC:
//                     needCategory2 = true; // 国内だけCATEGORY2必要
//                     break;
//                 case CATEGORY1_FOREIGN:
//                 case CATEGORY1_OVERSEAS:
//                 case CATEGORY1_INCOME:
//                 default:
//                     needCategory2 = false; // 海外/Overseas/収入金計上は不要（未知値も不要扱い）
//                     break;
//             }
//         }

//         String s1 = ctx.getSummary(1);
//         String s2 = ctx.getSummary(2);
//         String s3 = ctx.getSummary(3);
//         String s4 = ctx.getSummary(4);
//         String s5 = ctx.getSummary(5);
//         String s6 = ctx.getSummary(6);

//         List<String> accountCodes = tblSumRepository.findDistinctAccountCodesWithFilters(
//                 ctx.getCaseType(),
//                 ctx.getCategory1(),
//                 ctx.getCategory2(),
//                 nvl(s1),
//                 nvl(s2),
//                 nvl(s3),
//                 nvl(s4),
//                 nvl(s5),
//                 nvl(s6),
//                 ctx.getTransactionDate(),
//                 needCategory2);

//         List<SummaryResponse.CodeDto> codes = accountCodes == null
//                 ? List.of()
//                 : accountCodes.stream()
//                         .filter(v -> v != null && !v.isBlank())
//                         .distinct()
//                         .map(v -> new SummaryResponse.CodeDto(v, ""))
//                         .collect(Collectors.toList());

//         return SummaryResponse.success(codes);
//     }

//     private static String nvl(String s) {
//         return s == null ? "" : s.trim();
//     }
// }
