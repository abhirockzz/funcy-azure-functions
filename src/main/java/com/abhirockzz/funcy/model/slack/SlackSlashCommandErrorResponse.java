
package com.abhirockzz.funcy.model.slack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "response_type",
    "text"
})
public class SlackSlashCommandErrorResponse {

    @JsonProperty("response_type")
    private String response_type;
    @JsonProperty("text")
    private String text;

    public SlackSlashCommandErrorResponse() {
    }

    public SlackSlashCommandErrorResponse(String response_type, String text) {
        this.response_type = response_type;
        this.text = text;
    }
    
    

    @JsonProperty("response_type")
    public String getResponseType() {
        return response_type;
    }

    @JsonProperty("response_type")
    public void setResponseType(String response_type) {
        this.response_type = response_type;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

}
