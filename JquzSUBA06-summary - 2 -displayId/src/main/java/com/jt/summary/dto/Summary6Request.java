package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Summary6Request {

    private RequestBody requestBody;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestBody {

        @JsonProperty("Form")
        private FormDto form;

        @JsonProperty("case")
        private CaseDto caseDto;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormDto {

        @JsonProperty("F_meisai")
        private List<MeisaiRowDto> meisai;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class MeisaiRowDto {

        @JsonProperty("F_summary1")
        private String fSummary1;
        @JsonProperty("F_summary2")
        private String fSummary2;
        @JsonProperty("F_summary3")
        private String fSummary3;
        @JsonProperty("F_summary4")
        private String fSummary4;
        @JsonProperty("F_summary5")
        private String fSummary5;
        @JsonProperty("F_summary6")
        private String fSummary6;

        public String getFSummary1() {
            return fSummary1;
        }

        public String getFSummary2() {
            return fSummary2;
        }

        public String getFSummary3() {
            return fSummary3;
        }

        public String getFSummary4() {
            return fSummary4;
        }

        public String getFSummary5() {
            return fSummary5;
        }

        public String getFSummary6() {
            return fSummary6;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseDto {

        private String caseType;
        private CategoryDto categoryLevel1;
        private CategoryDto categoryLevel2;
        private ExtensionsDto extensions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        @JsonProperty("displayId")
        private String displayId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtensionsDto {

        @JsonProperty("TransactionDate")
        private String transactionDate;
    }
}
