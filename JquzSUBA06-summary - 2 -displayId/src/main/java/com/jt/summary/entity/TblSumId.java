package com.jt.summary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable

public class TblSumId implements Serializable {

  @Column(name = "case_type")
  private String caseType;

  @Column(name = "category1_displayid")
  private String category1DisplayId;

  @Column(name = "category2_displayid")
  private String category2DisplayId;

  @Column(name = "summary1")
  private String summary1;

  @Column(name = "summary2")
  private String summary2;

  @Column(name = "summary3")
  private String summary3;

  @Column(name = "summary4")
  private String summary4;

  @Column(name = "summary5")
  private String summary5;

  @Column(name = "summary6")
  private String summary6;

  @Column(name = "valid_from")
  private LocalDate validFrom;

  @Column(name = "valid_to")
  private LocalDate validTo;
}







// package com.jt.summary.entity;

// import jakarta.persistence.Column;
// import jakarta.persistence.Embeddable;
// import java.io.Serializable;
// import java.time.LocalDate;
// import lombok.*;

// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
// @EqualsAndHashCode
// @Embeddable
// public class TblSumId implements Serializable {

//   @Column(name = "CASE_TYPE")
//   private String caseType;

//   @Column(name = "CATEGORY1")
//   private String category1;

//   @Column(name = "CATEGORY2")
//   private String category2;

//   @Column(name = "SUMMARY1")
//   private String summary1;

//   @Column(name = "SUMMARY2")
//   private String summary2;

//   @Column(name = "SUMMARY3")
//   private String summary3;

//   @Column(name = "SUMMARY4")
//   private String summary4;

//   @Column(name = "SUMMARY5")
//   private String summary5;

//   @Column(name = "SUMMARY6")
//   private String summary6;

//   @Column(name = "VALID_FROM")
//   private LocalDate validFrom;

//   @Column(name = "VALID_TO")
//   private LocalDate validTo;
// }
