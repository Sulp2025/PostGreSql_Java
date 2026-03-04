package com.jt.summary.service;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;

public interface AccountCodeService {

    SummaryResponse getAccountCode(AccountCodeRequest request);
}
