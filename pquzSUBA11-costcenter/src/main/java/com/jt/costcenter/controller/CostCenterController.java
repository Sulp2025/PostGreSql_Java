package com.jt.costcenter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jt.costcenter.dto.request.CostCenterRequest;
import com.jt.costcenter.dto.response.CostCenterResponse;
import com.jt.costcenter.exception.BadRequestException;
import com.jt.costcenter.service.CostCenterService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Map;

@RestController
public class CostCenterController {

    private final CostCenterService service;

    public CostCenterController(CostCenterService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "running"));
    }

    @PostMapping("/costcenter")
    public ResponseEntity<CostCenterResponse> costCenter(@RequestBody(required = false) CostCenterRequest req) {
        /**
         * RequestBody / CaseData / Extensions 是独立类，不是 CostCenterRequest 的内部类
         */
        var extensionOpt = Optional.ofNullable(req)
                .map(CostCenterRequest::getRequestBody)
                .map(rb -> rb.getCaseData())
                .map(cd -> cd.getExtensions());

        String companyCode = extensionOpt
                .map(ext -> ext.getCompanyCode())
                .orElse(null);

        String transactionDateRaw = extensionOpt
                .map(ext -> ext.getTransactionDate())
                .orElse(null);

        if (companyCode == null || companyCode.isBlank()) {
            throw new BadRequestException("Missing CompanyCode");
        }

        if (transactionDateRaw == null || transactionDateRaw.isBlank()) {
            throw new BadRequestException("Missing TransactionDate");
        }

        LocalDate txDate;
        try {
            String datePart = transactionDateRaw.length() >= 10
                    ? transactionDateRaw.substring(0, 10)
                    : transactionDateRaw;

            txDate = LocalDate.parse(datePart);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "Invalid TransactionDate format, expected YYYY-MM-DD. Input: " + transactionDateRaw);
        }

        CostCenterResponse res = service.getCostCenters(companyCode, txDate);
        return ResponseEntity.ok(res);
    }
}