package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryResponse {

  private ResponseBody responseBody;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ResponseBody {
    private List<MessageDto> messages;
    private ValueDto value;

    @JsonProperty("isSuccess") 
    private Boolean isSuccess;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ValueDto {
    private List<CodeDto> codes;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CodeDto {
    private String key;
    private String description;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class MessageDto {
    private String code;
    private String message;
    private String type; // "S" / "E"
  }

  public static SummaryResponse success(List<CodeDto> codes) {
    List<MessageDto> msgs = List.of(new MessageDto("S000", "success", "INFO"));
    return new SummaryResponse(
        new ResponseBody(msgs, new ValueDto(codes), true));
  }

  public static SummaryResponse error(String msg) {
    return new SummaryResponse(
        new ResponseBody(
            List.of(new MessageDto("E400", msg, "E")),
            new ValueDto(Collections.emptyList()),
            true));
  }
}
