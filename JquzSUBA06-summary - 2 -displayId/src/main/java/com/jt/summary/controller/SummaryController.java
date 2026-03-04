package com.jt.summary.controller;

import com.jt.summary.dto.AccountCodeRequest;
import com.jt.summary.dto.CheckResponse;
import com.jt.summary.dto.Summary1Request;
import com.jt.summary.dto.Summary2Request;
import com.jt.summary.dto.Summary3Request;
import com.jt.summary.dto.Summary4Request;
import com.jt.summary.dto.Summary5Request;
import com.jt.summary.dto.Summary6Request;
import com.jt.summary.dto.SummaryResponse;
import com.jt.summary.service.AccountCodeService;
import com.jt.summary.service.DomeCurrAmtOthService;
import com.jt.summary.service.EnterExpensesService;
import com.jt.summary.service.ResearchService;
import com.jt.summary.service.Summary1Service;
import com.jt.summary.service.Summary2Service;
import com.jt.summary.service.Summary3Service;
import com.jt.summary.service.Summary4Service;
import com.jt.summary.service.Summary5Service;
import com.jt.summary.service.Summary6Service;
import com.jt.summary.service.DocCurrAmtService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SummaryController {

  private final Summary1Service summary1Service;
  private final Summary2Service summary2Service;
  private final Summary3Service summary3Service;
  private final Summary4Service summary4Service;
  private final Summary5Service summary5Service;
  private final Summary6Service summary6Service;
  private final AccountCodeService accountCodeService;
  private final EnterExpensesService enterExpensesService;
  private final ResearchService researchService;
  private final DomeCurrAmtOthService domeCurrAmtOthService;
  private final DocCurrAmtService DocCurrAmtService;

  @PostMapping("/summary1")
  public SummaryResponse summary1(@RequestBody Summary1Request request) {
    return summary1Service.getSummary1(request);
  }

  @PostMapping("/summary2")
  public SummaryResponse summary2(@RequestBody Summary2Request request) {
    return summary2Service.getSummary2(request);
  }

  @PostMapping("/summary3")
  public SummaryResponse summary3(@RequestBody Summary3Request request) {
    return summary3Service.getSummary3(request);
  }

  @PostMapping("/summary4")
  public SummaryResponse summary4(@RequestBody Summary4Request request) {
    return summary4Service.getSummary4(request);
  }

  @PostMapping("/summary5")
  public SummaryResponse summary5(@RequestBody Summary5Request request) {
    return summary5Service.getSummary5(request);
  }

  @PostMapping("/summary6")
  public SummaryResponse summary6(@RequestBody Summary6Request request) {
    return summary6Service.getSummary6(request);
  }

  @PostMapping("/accountcode")
  public SummaryResponse accountcode(@RequestBody AccountCodeRequest request) {
    return accountCodeService.getAccountCode(request);
  }

  @PostMapping("/enterexpenses")
  public SummaryResponse enterExpenses(@RequestBody AccountCodeRequest req) {
    return enterExpensesService.getEnterExpenses(req);
  }

  @PostMapping("/research")
  public SummaryResponse research(@RequestBody AccountCodeRequest req) {
    return researchService.getResearch(req);
  }

  @PostMapping("/domecurramtoth")
  public CheckResponse checkDomesticUnitPrice(@RequestBody AccountCodeRequest req) {
    return domeCurrAmtOthService.checkDomesticUnitPrice(req);
  }

  @PostMapping("/doccurramt")
  public CheckResponse checkJpyUnitPrice(@RequestBody AccountCodeRequest req) {
    return DocCurrAmtService.checkJpyPrice(req);
  }
}
