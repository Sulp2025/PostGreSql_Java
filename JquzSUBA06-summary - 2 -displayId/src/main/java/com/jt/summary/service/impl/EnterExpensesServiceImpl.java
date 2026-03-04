package com.jt.summary.service.impl;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.EnterExpensesService;
import com.jt.summary.util.RequestContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnterExpensesServiceImpl implements EnterExpensesService {

    private final TblSumRepository tblSumRepository;

    // MOD: use displayId + rules in application.yml to decide needCategory2
    private final Category2RuleService category2RuleService;

    @Override
    public SummaryResponse getEnterExpenses(AccountCodeRequest req) {

        RequestContext ctx = RequestContext.from(req);

        if (ctx.isMeisaiEmpty()) {
            return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
        }

        String caseType  = nvl(ctx.getCaseType());

        // MOD: ctx.getCategory1()/getCategory2() already store displayId
        String category1DisplayId = nvl(ctx.getCategory1());
        String category2DisplayId = nvl(ctx.getCategory2());

        // MOD: determine needCategory2 by (caseType + category1_displayid)
        boolean needCategory2 = category2RuleService.needCategory2(caseType, category1DisplayId);

        // MOD: when needCategory2==true, category2_displayid is required
        if (needCategory2 && category2DisplayId.isBlank()) {
            return SummaryResponse.error("requestBody.case.categoryLevel2.displayId is required");
        }

        // summary1 is optional (keep original behavior)
        String s1 = ctx.getSummary(1);
        String s2 = ctx.getSummary(2);
        String s3 = ctx.getSummary(3);
        String s4 = ctx.getSummary(4);
        String s5 = ctx.getSummary(5);
        String s6 = ctx.getSummary(6);

        List<String> values = tblSumRepository.findDistinctEnterExpensesWithFilters(
                caseType,
                category1DisplayId,
                // MOD: pass category2 only when needed
                needCategory2 ? category2DisplayId : null,
                nvl(s1), nvl(s2), nvl(s3), nvl(s4), nvl(s5), nvl(s6),
                ctx.getTransactionDate(),
                needCategory2
        );

        var codes = (values == null ? List.<String>of() : values)
                .stream()
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .map(v -> new SummaryResponse.CodeDto(v, "")) // key=v, description空
                .toList();

        return SummaryResponse.success(codes);
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}


// package com.jt.summary.service.impl;

// import com.jt.summary.dto.AccountCodeRequest;
// import com.jt.summary.dto.SummaryResponse;
// import com.jt.summary.repository.TblSumRepository;
// import com.jt.summary.service.EnterExpensesService;
// import com.jt.summary.util.RequestContext;
// import java.util.List;
// import java.util.Set;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor
// public class EnterExpensesServiceImpl implements EnterExpensesService {

//     // 固定資産（Fixed Asset）系の caseType
//     private static final Set<String> CASETYPE_ASSET_CATE = Set.of("ZD11");

//     // Category Level1
//     private static final String CATEGORY1_DOMESTIC = "国内";
//     private static final String CATEGORY1_FOREIGN  = "海外";
//     private static final String CATEGORY1_OVERSEAS = "Overseas";
//     private static final String CATEGORY1_INCOME   = "収入金計上";

//     private final TblSumRepository tblSumRepository;

//     @Override
//     public SummaryResponse getEnterExpenses(AccountCodeRequest req) {

//         RequestContext ctx = RequestContext.from(req);

//         if (ctx.isMeisaiEmpty()) {
//             return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
//         }

//         String caseType   = nvl(ctx.getCaseType());
//         String category1  = nvl(ctx.getCategory1());
//         String category2  = nvl(ctx.getCategory2());

//         boolean isFixedAsset = CASETYPE_ASSET_CATE.contains(caseType);

//         boolean isDomestic = CATEGORY1_DOMESTIC.equals(category1);
//         boolean isOverseas = CATEGORY1_FOREIGN.equals(category1) || CATEGORY1_OVERSEAS.equals(category1);
//         boolean isIncome   = CATEGORY1_INCOME.equals(category1);

//         //  needCategory2 规则：
//         // - 固定資産：无论国内/海外/Overseas/収入金計上，都需要CATEGORY2
//         // - 非固定資産：只有国内需要CATEGORY2（海外/Overseas/収入金計上 不需要）
//         boolean needCategory2;
//         if (isFixedAsset) {
//             needCategory2 = true;
//         } else if (isIncome || isOverseas) {
//             needCategory2 = false;
//         } else {
//             needCategory2 = isDomestic;
//         }

//         //  当 needCategory2==true 时，CATEGORY2 必填
//         if (needCategory2 && category2.isBlank()) {
//             return SummaryResponse.error("requestBody.Form.F_meisai[0].F_category2 is required");
//         }

//         //  summary1 改为可选：不再要求必填
//         String s1 = ctx.getSummary(1);
//         String s2 = ctx.getSummary(2);
//         String s3 = ctx.getSummary(3);
//         String s4 = ctx.getSummary(4);
//         String s5 = ctx.getSummary(5);
//         String s6 = ctx.getSummary(6);

//         List<String> values = tblSumRepository.findDistinctEnterExpensesWithFilters(
//                 caseType,
//                 category1,
//                 category2,
//                 nvl(s1), nvl(s2), nvl(s3), nvl(s4), nvl(s5), nvl(s6),
//                 ctx.getTransactionDate(),
//                 needCategory2
//         );

//         var codes = (values == null ? List.<String>of() : values)
//                 .stream()
//                 .filter(v -> v != null && !v.isBlank())
//                 .distinct()
//                 .map(v -> new SummaryResponse.CodeDto(v, "")) // key=v, description空
//                 .toList();

//         return SummaryResponse.success(codes);
//     }

//     private static String nvl(String s) {
//         return s == null ? "" : s.trim();
//     }
// }
