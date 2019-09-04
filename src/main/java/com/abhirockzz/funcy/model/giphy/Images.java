package com.abhirockzz.funcy.model.giphy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "downsized"

})
public class Images {

    @JsonProperty("downsized")
    private Downsized downsized;

    @JsonProperty("downsized")
    public Downsized getDownsized() {
        return downsized;
    }

    @JsonProperty("downsized")
    public void setDownsized(Downsized downsized) {
        this.downsized = downsized;
    }

}
