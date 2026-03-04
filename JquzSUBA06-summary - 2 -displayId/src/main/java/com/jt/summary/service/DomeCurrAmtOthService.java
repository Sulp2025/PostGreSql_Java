package com.jt.summary.service;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.CheckResponse;

public interface DomeCurrAmtOthService {

    CheckResponse checkDomesticUnitPrice(AccountCodeRequest request);

}
