package com.jt.summary.repository;

import com.jt.summary.entity.TblSum;
import com.jt.summary.entity.TblSumId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TblSumRepository extends JpaRepository<TblSum, TblSumId> {

  @Query("""
        select distinct t.id.summary1
        from TblSum t
        where t.id.caseType = :caseType
          and t.id.category1DisplayId = :category1DisplayId
          and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,''))
          and :txDate between t.id.validFrom and t.id.validTo
          and t.id.summary1 is not null
        order by t.id.summary1
      """)
  List<String> findDistinctSummary1(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * summary2
   */
  @Query("""
        select distinct t.id.summary2
        from TblSum t
        where t.id.caseType = :caseType
          and t.id.category1DisplayId =:category1DisplayId
          and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
          and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
          and :txDate between t.id.validFrom and t.id.validTo
          and t.id.summary2 is not null
        order by t.id.summary2
      """)
  List<String> findDistinctSummary2(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * summary3
   */
  @Query("""
        select distinct t.id.summary3
        from TblSum t
        where t.id.caseType = :caseType
          and t.id.category1DisplayId =:category1DisplayId
          and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
          and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
          and (:summary2 is null or :summary2 = '' or t.id.summary2 = :summary2)
          and :txDate between t.id.validFrom and t.id.validTo
          and t.id.summary3 is not null
        order by t.id.summary3
      """)
  List<String> findDistinctSummary3(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * summary4
   */
  @Query("""
          select distinct t.id.summary4
          from TblSum t
          where t.id.caseType = :caseType
            and t.id.category1DisplayId =:category1DisplayId
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
            and (:summary2 is null or t.id.summary2 = :summary2)
            and (:summary3 is null or t.id.summary3 = :summary3)
            and :txDate between t.id.validFrom and t.id.validTo
            and t.id.summary4 is not null
          order by t.id.summary4
      """)
  List<String> findDistinctSummary4Optional(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * summary5
   */
  @Query("""
          select distinct t.id.summary5
          from TblSum t
          where t.id.caseType = :caseType
            and t.id.category1DisplayId =:category1DisplayId
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
            and (:summary2 is null or t.id.summary2 = :summary2)
            and (:summary3 is null or t.id.summary3 = :summary3)
            and (:summary4 is null or t.id.summary4 = :summary4)
            and :txDate between t.id.validFrom and t.id.validTo
            and t.id.summary5 is not null
          order by t.id.summary5
      """)
  List<String> findDistinctSummary5Optional(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * summary6
   */
  @Query("""
          select distinct t.id.summary6
          from TblSum t
          where t.id.caseType = :caseType
            and t.id.category1DisplayId =:category1DisplayId
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
            and (:summary2 is null or t.id.summary2 = :summary2)
            and (:summary3 is null or t.id.summary3 = :summary3)
            and (:summary4 is null or t.id.summary4 = :summary4)
            and (:summary5 is null or t.id.summary5 = :summary5)
            and :txDate between t.id.validFrom and t.id.validTo
            and t.id.summary6 is not null
          order by t.id.summary6
      """)
  List<String> findDistinctSummary6Optional(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * accountCode
   */
  @Query("""
          select distinct t.accountCode
          from TblSum t
          where (:caseType is null or t.id.caseType = :caseType)
            and (:category1DisplayId is null or t.id.category1DisplayId = :category1DisplayId) 
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or coalesce(t.id.summary1,'') = coalesce(:summary1,''))
            and (:summary2 is null or :summary2 = '' or coalesce(t.id.summary2,'') = coalesce(:summary2,''))
            and (:summary3 is null or :summary3 = '' or coalesce(t.id.summary3,'') = coalesce(:summary3,''))
            and (:summary4 is null or :summary4 = '' or coalesce(t.id.summary4,'') = coalesce(:summary4,''))
            and (:summary5 is null or :summary5 = '' or coalesce(t.id.summary5,'') = coalesce(:summary5,''))
            and (:summary6 is null or :summary6 = '' or coalesce(t.id.summary6,'') = coalesce(:summary6,''))
            and :txDate between t.id.validFrom and t.id.validTo
          order by t.accountCode
      """)
  List<String> findDistinctAccountCodesWithFilters(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("summary6") String summary6,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  // EnterExpenses 下拉
  @Query("""
          select distinct t.enterExpenses
          from TblSum t
          where (:caseType is null or t.id.caseType = :caseType)
            and (:category1DisplayId is null or t.id.category1DisplayId = :category1DisplayId) 
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or coalesce(t.id.summary1,'') = coalesce(:summary1,''))
            and (:summary2 is null or :summary2 = '' or coalesce(t.id.summary2,'') = coalesce(:summary2,''))
            and (:summary3 is null or :summary3 = '' or coalesce(t.id.summary3,'') = coalesce(:summary3,''))
            and (:summary4 is null or :summary4 = '' or coalesce(t.id.summary4,'') = coalesce(:summary4,''))
            and (:summary5 is null or :summary5 = '' or coalesce(t.id.summary5,'') = coalesce(:summary5,''))
            and (:summary6 is null or :summary6 = '' or coalesce(t.id.summary6,'') = coalesce(:summary6,''))
            and :txDate between t.id.validFrom and t.id.validTo
          order by t.enterExpenses
      """)
  List<String> findDistinctEnterExpensesWithFilters(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("summary6") String summary6,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  /**
   * Research 下拉 NULL/空字符串同类处理，都会参与过滤
   * @param caseType
   * @param category1DisplayId
   * @param category2DisplayId
   * @param summary1
   * @param summary2
   * @param summary3
   * @param summary4
   * @param summary5
   * @param summary6
   * @param txDate
   * @param needCategory2
   * @return
   */
  @Query("""
          select distinct t.research
          from TblSum t
          where (:caseType is null or t.id.caseType = :caseType)
            and (:category1DisplayId is null or t.id.category1DisplayId = :category1DisplayId) 
            and (:needCategory2 = false or coalesce(t.id.category2DisplayId,'') = coalesce(:category2DisplayId,'')) 
            and (:summary1 is null or :summary1 = '' or coalesce(t.id.summary1,'') = coalesce(:summary1,''))
            and (:summary2 is null or :summary2 = '' or coalesce(t.id.summary2,'') = coalesce(:summary2,''))
            and (:summary3 is null or :summary3 = '' or coalesce(t.id.summary3,'') = coalesce(:summary3,''))
            and (:summary4 is null or :summary4 = '' or coalesce(t.id.summary4,'') = coalesce(:summary4,''))
            and (:summary5 is null or :summary5 = '' or coalesce(t.id.summary5,'') = coalesce(:summary5,''))
            and (:summary6 is null or :summary6 = '' or coalesce(t.id.summary6,'') = coalesce(:summary6,''))
            and :txDate between t.id.validFrom and t.id.validTo
            and t.research is not null
          order by t.research
      """)
  List<String> findDistinctResearchWithFilters(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("summary6") String summary6,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  // ======================================================================
  // max
  // ======================================================================
  @Query("""
        select max(t.maxAmount)
          from TblSum t
         where t.id.caseType = :caseType
           and t.id.category1DisplayId =:category1DisplayId
           and (:needCategory2 = false
                or coalesce(t.id.category2DisplayId, '') = coalesce(:category2DisplayId, '')) 
           and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
           and (:summary2 is null or t.id.summary2 = :summary2)
           and (:summary3 is null or t.id.summary3 = :summary3)
           and (:summary4 is null or t.id.summary4 = :summary4)
           and (:summary5 is null or t.id.summary5 = :summary5)
           and (:summary6 is null or t.id.summary6 = :summary6)
           and (:accountCode is null or t.accountCode = :accountCode)
           and (:enterExpenses is null or t.enterExpenses = :enterExpenses)
           and :txDate between t.id.validFrom and t.id.validTo
      """)
  BigDecimal findMaxAmountWithFilters(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("summary6") String summary6,
      @Param("accountCode") String accountCode,
      @Param("enterExpenses") String enterExpenses,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);

  // ======================================================================
  // min：改成和 max 完全同一套 WHERE 逻辑
  // ======================================================================
  @Query("""
        select min(t.minAmount)
          from TblSum t
         where t.id.caseType = :caseType
           and t.id.category1DisplayId =:category1DisplayId
           and (:needCategory2 = false
                or coalesce(t.id.category2DisplayId, '') = coalesce(:category2DisplayId, ''))
           and (:summary1 is null or :summary1 = '' or t.id.summary1 = :summary1)
           and (:summary2 is null or t.id.summary2 = :summary2)
           and (:summary3 is null or t.id.summary3 = :summary3)
           and (:summary4 is null or t.id.summary4 = :summary4)
           and (:summary5 is null or t.id.summary5 = :summary5)
           and (:summary6 is null or t.id.summary6 = :summary6)
           and (:accountCode is null or t.accountCode = :accountCode)
           and (:enterExpenses is null or t.enterExpenses = :enterExpenses)
           and :txDate between t.id.validFrom and t.id.validTo
      """)
  BigDecimal findMinAmountWithFilters(
      @Param("caseType") String caseType,
      @Param("category1DisplayId") String category1DisplayId,
      @Param("category2DisplayId") String category2DisplayId,
      @Param("summary1") String summary1,
      @Param("summary2") String summary2,
      @Param("summary3") String summary3,
      @Param("summary4") String summary4,
      @Param("summary5") String summary5,
      @Param("summary6") String summary6,
      @Param("accountCode") String accountCode,
      @Param("enterExpenses") String enterExpenses,
      @Param("txDate") LocalDate txDate,
      @Param("needCategory2") boolean needCategory2);
}