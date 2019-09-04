
package com.abhirockzz.funcy.model.slack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "text",
    "image_url"
})
public class Attachment {

    @JsonProperty("text")
    private String text;
    @JsonProperty("image_url")
    private String image_url;

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("image_url")
    public String getImageUrl() {
        return image_url;
    }

    @JsonProperty("image_url")
    public void setImageUrl(String imageUrl) {
        this.image_url = imageUrl;
    }

}
