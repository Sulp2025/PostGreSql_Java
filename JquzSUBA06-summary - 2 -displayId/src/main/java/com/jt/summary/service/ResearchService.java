package com.jt.summary.service;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;

public interface ResearchService {
    SummaryResponse getResearch(AccountCodeRequest request);
}
