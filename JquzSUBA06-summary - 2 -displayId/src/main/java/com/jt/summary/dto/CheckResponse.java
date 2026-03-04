package com.jt.summary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckResponse {

  @JsonProperty("responseBody")
  private ResponseBody responseBody;

  public static CheckResponse fromCheckResult(boolean hasError, String msg) {

    Form form = new Form(!hasError, msg); // hasError=true → F_FormErrorCheck=false
    Value value = new Value(form);

    List<Message> messages = new ArrayList<>();
    // if (hasError) {
    //   messages.add(new Message("E400", "Error", "INFO"));
    // } else {
    //   messages.add(new Message("S000", "Success", "INFO"));
    // }
    // ResponseBody body = new ResponseBody(new ArrayList<>(), value, true);
    ResponseBody body = new ResponseBody(messages, value, true);
    return new CheckResponse(body);
  }

  // ---------------- inner classes ----------------

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ResponseBody {
    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("value")
    private Value value;

    @JsonProperty("isSuccess")
    private Boolean isSuccess;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Value {
    @JsonProperty("Form")
    private Form form;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Form {
    @JsonProperty("F_FormErrorCheck")
    private boolean formErrorCheck;

    @JsonProperty("F_FormErrorMsg")
    private String formErrorMsg;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Message {
    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    private String type;
  }
}
