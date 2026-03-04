package com.jt.summary.util;

import com.jt.summary.dto.AccountCodeRequest;
import java.time.LocalDate;
import java.util.List;

// MOD: add import (previously commented out -> compile error)
//import com.jt.summary.util.DateParsers;

public class RequestContext {

    private final String caseType;
    private final String category1; // category1_displayid
    private final String category2; // category2_displayid
    private final LocalDate transactionDate;
    private final List<AccountCodeRequest.MeisaiRowDto> meisai;

    private RequestContext(
            String caseType,
            String category1,
            String category2,
            LocalDate transactionDate,
            List<AccountCodeRequest.MeisaiRowDto> meisai) {
        this.caseType = caseType;
        this.category1 = category1;
        this.category2 = category2;
        this.transactionDate = transactionDate;
        this.meisai = meisai;
    }

    public static RequestContext from(AccountCodeRequest req) {

        // MOD: consistent default values
        String caseType = null;
        String category1 = null;
        String category2 = null;
        LocalDate txDate = null;
        List<AccountCodeRequest.MeisaiRowDto> meisai = List.of();

        if (req == null || req.getRequestBody() == null) {
            // MOD: return default with null strings + today date + empty list
            return new RequestContext(null, null, null, LocalDate.now(), List.of());
        }

        var rb = req.getRequestBody();

        if (rb.getCaseDto() != null) {
            caseType = trimToNull(rb.getCaseDto().getCaseType()); // MOD

            // MOD: use displayId (align with DB columns category1_displayid/category2_displayid)
            if (rb.getCaseDto().getCategoryLevel1() != null) {
                category1 = trimToNull(rb.getCaseDto().getCategoryLevel1().getDisplayId()); // MOD
            }
            if (rb.getCaseDto().getCategoryLevel2() != null) {
                category2 = trimToNull(rb.getCaseDto().getCategoryLevel2().getDisplayId()); // MOD
            }

            if (rb.getCaseDto().getExtensions() != null) {
                // TransactionDate accepted as String (multiple formats) -> parse to LocalDate.
                String raw = rb.getCaseDto().getExtensions().getTransactionDate();
                raw = trimToNull(raw); // MOD
                if (raw != null) {
                    try {
                        txDate = DateParsers.parseFlexible(raw); // MOD: DateParsers import enabled
                    } catch (IllegalArgumentException ignored) {
                        // keep null; fallback below
                    }
                }
            }
        }

        // MOD: keep meisai extraction independent and safe
        if (rb.getForm() != null && rb.getForm().getMeisai() != null) {
            meisai = rb.getForm().getMeisai();
        }

        // MOD: single place fallback for date
        if (txDate == null) {
            txDate = LocalDate.now();
        }

        return new RequestContext(caseType, category1, category2, txDate, meisai); // MOD: already trimmed
    }

    // MOD: trim empty -> null (helps downstream checks and query conditions)
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public boolean isMeisaiEmpty() {
        return meisai == null || meisai.isEmpty();
    }

    public String getSummary(int level) {
        if (meisai == null) {
            return null;
        }

        for (var row : meisai) {
            if (row == null) {
                continue;
            }

            String v = switch (level) {
                case 1 -> row.getFSummary1();
                case 2 -> row.getFSummary2();
                case 3 -> row.getFSummary3();
                case 4 -> row.getFSummary4();
                case 5 -> row.getFSummary5();
                case 6 -> row.getFSummary6();
                default -> null;
            };

            v = trimToNull(v); // MOD
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getCategory1() {
        return category1;
    }

    public String getCategory2() {
        return category2;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public List<AccountCodeRequest.MeisaiRowDto> getMeisai() {
        return meisai;
    }
}



// package com.jt.summary.util;

// import com.jt.summary.dto.AccountCodeRequest;
// // import com.jt.summary.util.DateParsers;
// import java.time.LocalDate;

// //import java.time.format.DateTimeFormatter;
// //import java.time.format.DateTimeParseException;
// import java.util.List;

// public class RequestContext {

//     private final String caseType;
//     private final String category1;
//     private final String category2;
//     private final LocalDate transactionDate;
//     private final List<AccountCodeRequest.MeisaiRowDto> meisai;

//     private RequestContext(
//             String caseType,
//             String category1,
//             String category2,
//             LocalDate transactionDate,
//             List<AccountCodeRequest.MeisaiRowDto> meisai) {
//         this.caseType = caseType;
//         this.category1 = category1;
//         this.category2 = category2;
//         this.transactionDate = transactionDate;
//         this.meisai = meisai;
//     }

//     public static RequestContext from(AccountCodeRequest req) {
//         if (req == null || req.getRequestBody() == null) {
//             return new RequestContext(null, null, null, LocalDate.now(), List.of());
//         }

//         var rb = req.getRequestBody();

//         String caseType = null;
//         String category1 = null;
//         String category2 = null;
//         LocalDate txDate = null;

//         if (rb.getCaseDto() != null) {
//             caseType = rb.getCaseDto().getCaseType();

//             // if (rb.getCaseDto().getCategoryLevel1() != null) {
//             // // category1 = trim(rb.getCaseDto().getCategoryLevel1().getName());

//             // }
//             // if (rb.getCaseDto().getCategoryLevel2() != null) {
//             // category2 = trim(rb.getCaseDto().getCategoryLevel2().getName());
//             // }

//             // MOD: use displayId (align with DB columns
//             // category1_displayid/category2_displayid)
//             if (rb.getCaseDto().getCategoryLevel1() != null) {
//                 category1 = trim(rb.getCaseDto().getCategoryLevel1().getDisplayId()); // MOD
//             }
//             if (rb.getCaseDto().getCategoryLevel2() != null) {
//                 category2 = trim(rb.getCaseDto().getCategoryLevel2().getDisplayId()); // MOD
//             }

//             if (rb.getCaseDto().getExtensions() != null) {
//                 // TransactionDate is accepted as String (multiple formats) -> parse to
//                 // LocalDate.
//                 String raw = rb.getCaseDto().getExtensions().getTransactionDate();
//                 if (raw != null && !raw.trim().isEmpty()) {
//                     try {
//                         txDate = DateParsers.parseFlexible(raw);
//                     } catch (IllegalArgumentException ignored) {
//                         // keep null; services can decide how to handle missing/invalid date
//                     }
//                 }
//             }
//         }

//         List<AccountCodeRequest.MeisaiRowDto> meisai = rb.getForm() == null || rb.getForm().getMeisai() == null
//                 ? List.of()
//                 : rb.getForm().getMeisai();

//         if (txDate == null) {
//             txDate = LocalDate.now();
//         }

//         return new RequestContext(trim(caseType), trim(category1), trim(category2), txDate, meisai);
//     }

//     private static String trim(String s) {
//         return s == null ? null : s.trim();
//     }

//     public boolean isMeisaiEmpty() {
//         return meisai == null || meisai.isEmpty();
//     }

//     public String getSummary(int level) {
//         if (meisai == null) {
//             return null;
//         }

//         for (var row : meisai) {
//             if (row == null) {
//                 continue;
//             }

//             String v = switch (level) {
//                 case 1 -> row.getFSummary1();
//                 case 2 -> row.getFSummary2();
//                 case 3 -> row.getFSummary3();
//                 case 4 -> row.getFSummary4();
//                 case 5 -> row.getFSummary5();
//                 case 6 -> row.getFSummary6();
//                 default -> null;
//             };

//             if (v != null && !v.trim().isEmpty()) {
//                 return v.trim();
//             }
//         }
//         return null;
//     }

//     public String getCaseType() {
//         return caseType;
//     }

//     public String getCategory1() {
//         return category1;
//     }

//     public String getCategory2() {
//         return category2;
//     }

//     public LocalDate getTransactionDate() {
//         return transactionDate;
//     }

//     public List<AccountCodeRequest.MeisaiRowDto> getMeisai() {
//         return meisai;
//     }
// }
