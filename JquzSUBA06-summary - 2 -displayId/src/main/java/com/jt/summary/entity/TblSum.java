package com.jt.summary.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


//schema = "public"
@Entity
@Table(name = "zquc004")
@Getter
@Setter
@NoArgsConstructor
public class TblSum implements Serializable {

  @EmbeddedId
  private TblSumId id;

  @Column(name = "accountcode")
  private String accountCode;

  @Column(name = "enter_expenses")
  private String enterExpenses;

  @Column(name = "research")
  private String research;

  @Column(name = "min_amount")
  private BigDecimal minAmount;

  @Column(name = "max_amount")
  private BigDecimal maxAmount;
}



// package com.jt.summary.entity;

// import java.io.Serializable;
// import java.math.BigDecimal;

// import jakarta.persistence.*;
// import lombok.*;

// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @Entity
// @Table(name = "TBL_SUM")
// public class TblSum implements Serializable {
//     @EmbeddedId

//     private TblSumId id;

//     @Column(name = "ACCOUNTCODE")
//     private String accountCode;

//     @Column(name = "ENTER_EXPENSES")
//     private String enterExpenses;

//     @Column(name = "RESEARCH")
//     private String research;

//     @Column(name = "MIN_AMOUNT")
//     private BigDecimal minAmount;

//     @Column(name = "MAX_AMOUNT")
//     private BigDecimal maxAmount;
// }
