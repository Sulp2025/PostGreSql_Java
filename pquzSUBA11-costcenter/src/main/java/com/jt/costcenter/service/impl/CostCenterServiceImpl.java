package com.jt.costcenter.service.impl;

import com.jt.costcenter.dto.response.CodeItem;
import com.jt.costcenter.dto.response.CostCenterResponse;
import com.jt.costcenter.dto.response.Message;
import com.jt.costcenter.dto.response.ResponseBody;
import com.jt.costcenter.dto.response.Value;
import com.jt.costcenter.exception.NotFoundException;
import com.jt.costcenter.model.CostCenterRow;
import com.jt.costcenter.repository.CostCenterRepository;
import com.jt.costcenter.service.CostCenterService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository repository;

    public CostCenterServiceImpl(CostCenterRepository repository) {
        this.repository = repository;
    }

    @Override
    public CostCenterResponse getCostCenters(String companyCode, LocalDate transactionDate) {

        List<CostCenterRow> rows = repository.findValidCostCenters(companyCode, transactionDate);

        if (rows.isEmpty()) {
            throw new NotFoundException("No CostCenter found");
        }

        /**
         * 规则：key = "(" + COSTCENTER + ")" + COSTCENTER_DESCRIPTION，description = ""
         * CodeItem 是独立类
         * Message / Value / ResponseBody 都是独立类
         */

        List<CodeItem> codes = rows.stream()
                .map(r -> {
                    String ccCode = (r.getCostCenter() == null) ? "" : r.getCostCenter().trim();
                    String ccDesc = (r.getCostCenterDescription() == null) ? "" : r.getCostCenterDescription().trim();
                    // key 格式 = "(" + costcenter + ")" + costcenter_description
                    String displayText = "(" + ccCode + ")" + ccDesc;

                    return new CodeItem(displayText, "");
                })
                .toList();

        Message msg = new Message("S000", "Success", "INFO");
        Value value = new Value(codes);
        ResponseBody body = new ResponseBody(List.of(msg), value, true);

        return new CostCenterResponse(body);
    }
}
