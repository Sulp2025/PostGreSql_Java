package com.jt.summary.service;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.SummaryResponse;

public interface SummaryDropdownService {
  SummaryResponse getSummaryOptions(int level, AccountCodeRequest req);
}