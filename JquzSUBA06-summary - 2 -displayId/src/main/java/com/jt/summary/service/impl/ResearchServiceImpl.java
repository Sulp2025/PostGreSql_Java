package com.jt.summary.service.impl;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.repository.TblSumRepository;
import com.jt.summary.service.ResearchService;
import com.jt.summary.util.RequestContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResearchServiceImpl implements ResearchService {

    private final TblSumRepository tblSumRepository;

    // MOD: use displayId + rules in application.yml to decide needCategory2
    private final Category2RuleService category2RuleService;

    @Override
    public SummaryResponse getResearch(AccountCodeRequest req) {

        RequestContext ctx = RequestContext.from(req);

        if (ctx.isMeisaiEmpty()) {
            return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
        }

        // MOD: ctx.getCategory1()/getCategory2() already store displayId
        String caseType = nvl(ctx.getCaseType());
        String category1DisplayId = nvl(ctx.getCategory1());
        String category2DisplayId = nvl(ctx.getCategory2());

        // MOD: determine needCategory2 by (caseType + category1_displayid)
        boolean needCategory2 = category2RuleService.needCategory2(caseType, category1DisplayId);

        // MOD: when needCategory2==true, category2_displayid is required
        if (needCategory2 && category2DisplayId.isBlank()) {
            return SummaryResponse.error("requestBody.case.categoryLevel2.displayId is required");
        }

        String s1 = ctx.getSummary(1);
        String s2 = ctx.getSummary(2);
        String s3 = ctx.getSummary(3);
        String s4 = ctx.getSummary(4);
        String s5 = ctx.getSummary(5);
        String s6 = ctx.getSummary(6);

        List<String> values =
                tblSumRepository.findDistinctResearchWithFilters(
                        caseType,                         // MOD: use trimmed local var
                        category1DisplayId,                // MOD: displayId
                        needCategory2 ? category2DisplayId : null, // MOD: category2 only if needed
                        nvl(s1),
                        nvl(s2),
                        nvl(s3),
                        nvl(s4),
                        nvl(s5),
                        nvl(s6),
                        ctx.getTransactionDate(),
                        needCategory2
                );

        List<SummaryResponse.CodeDto> codes =
                values == null
                        ? List.of()
                        : values.stream()
                                .filter(v -> v != null && !v.isBlank())
                                .distinct()
                                .map(v -> new SummaryResponse.CodeDto(v, "")) // descriptionは空で踏襲
                                .collect(Collectors.toList());

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
// import com.jt.summary.service.ResearchService;
// import com.jt.summary.util.RequestContext;
// import java.util.List;
// import java.util.Set;
// import java.util.stream.Collectors;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor
// public class ResearchServiceImpl implements ResearchService {

//     private static final String CATEGORY1_DOMESTIC = "国内";
//     private static final String CATEGORY1_FOREIGN = "海外";
//     private static final String CATEGORY1_OVERSEAS = "Overseas";
//     private static final String CATEGORY1_INCOME = "収入金計上";

//     // 固定資産（Fixed Asset）系の caseType
//     private static final Set<String> CASETYPE_ASSET_CATE = Set.of("ZD11");

//     private final TblSumRepository tblSumRepository;

//     @Override
//     public SummaryResponse getResearch(AccountCodeRequest req) {

//         RequestContext ctx = RequestContext.from(req);

//         if (ctx.isMeisaiEmpty()) {
//             return SummaryResponse.error("requestBody.Form.F_meisai[0] is required");
//         }

//         boolean isDomestic = CATEGORY1_DOMESTIC.equals(ctx.getCategory1());
//         boolean isOverseas = CATEGORY1_FOREIGN.equals(ctx.getCategory1())
//                 || CATEGORY1_OVERSEAS.equals(ctx.getCategory1());
//         boolean isIncome = CATEGORY1_INCOME.equals(ctx.getCategory1());

//         boolean needCategory2;
//         if (isIncome) {
//             needCategory2 = false;
//         } else if (CASETYPE_ASSET_CATE.contains(ctx.getCaseType())) {
//             // 固定資産(ZD11)は国内/海外どちらでもCategory2が必要 
//             needCategory2 = isDomestic || isOverseas;
//         } else if (isOverseas) {
//             needCategory2 = false;
//         } else {
//             needCategory2 = isDomestic;
//         }

//         String s1 = ctx.getSummary(1);
//         String s2 = ctx.getSummary(2);
//         String s3 = ctx.getSummary(3);
//         String s4 = ctx.getSummary(4);
//         String s5 = ctx.getSummary(5);
//         String s6 = ctx.getSummary(6);

//         List<String> values =
//                 tblSumRepository.findDistinctResearchWithFilters(
//                         ctx.getCaseType(),
//                         ctx.getCategory1(),
//                         ctx.getCategory2(),
//                         nvl(s1), 
//                         nvl(s2),
//                         nvl(s3),
//                         nvl(s4),
//                         nvl(s5),
//                         nvl(s6),
//                         ctx.getTransactionDate(),
//                         needCategory2
//                 );

//         List<SummaryResponse.CodeDto> codes =
//                 values == null
//                         ? List.of()
//                         : values.stream()
//                                 .filter(v -> v != null && !v.isBlank())
//                                 .distinct()
//                                 .map(v -> new SummaryResponse.CodeDto(v, "")) // descriptionは空で踏襲
//                                 .collect(Collectors.toList());

//         return SummaryResponse.success(codes);
//     }

//     private static String nvl(String s) {
//         return s == null ? "" : s.trim();
//     }
// }
