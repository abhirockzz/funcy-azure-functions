package com.abhirockzz.funcy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import com.abhirockzz.funcy.model.giphy.GiphyRandomAPIGetResponse;
import com.abhirockzz.funcy.model.slack.Attachment;
import com.abhirockzz.funcy.model.slack.SlackSlashCommandErrorResponse;
import com.abhirockzz.funcy.model.slack.SlackSlashCommandResponse;

import java.io.IOException;
import java.util.*;
import java.net.URLDecoder;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Contains core logic to power the function for the 'funcy' Slack Slash command
 */
public class Funcy {

    private static Logger LOGGER;
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SLACK_RESPONSE_STATIC_TEXT = "*With* :heart: *from /funcy*";
    private static CloseableHttpClient HTTP_CLIENT = null;

    /**
     * Entry point for the function
     *
     * @param request HttpRequestMessage injected at runtime
     * @param context ExecutionContext injected at runtime
     * @return HttpResponseMessage sent to Slack - error or a random GIPHY image
     *
     */
    @FunctionName("funcy")
    public HttpResponseMessage handleSlackSlashCommand(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        LOGGER = context.getLogger();
        //HttpResponseMessage errorResponse = request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();

        //as per https://api.slack.com/slash-commands#responding_with_errors
        HttpResponseMessage errorResponse = request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-type", "application/json")
                .body(new SlackSlashCommandErrorResponse("ephemeral", "Sorry, that didn't work. Please try again.")).build();

        HttpResponseMessage missingKeywordResponse = request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-type", "application/json")
                .body(new SlackSlashCommandErrorResponse("ephemeral", "Please include a keyword with your slash command e.g. /funcy cat")).build();

        String slackData = request.getBody().get();
        String decodedSlackData = null;

        Map<String, String> slackdataMap = new HashMap<>();
        try {
            //slack data content type is application/x-www-form-urlencoded and needs to be decoded first
            decodedSlackData = URLDecoder.decode(slackData, "UTF-8");
        } catch (Exception ex) {
            LOGGER.severe("Unable to decode data sent by Slack - " + ex.getMessage());
            return errorResponse;
        }
        //Arrays.stream(decodedSlackData.split("&")).forEach((kv) -> slackdataMap.put(kv.split("=")[0], kv.split("=")[1]));

        //parse the decoded slack data into a usable HashMap
        for (String kv : decodedSlackData.split("&")) {
            try {
                slackdataMap.put(kv.split("=")[0], kv.split("=")[1]);
            } catch (Exception e) {
                /*
                probably because some value in blank - most likely 'text' (if user does not send keyword with slash command).
                skip that and continue processing other attrbiutes in slack data
                 */
            }
        }

        //check whether user sent a keyword with the command i.e. /funcy should be followed by a search term
        if (!slackdataMap.containsKey("text")) {
            LOGGER.severe("User did not send a keyword with slash command");
            return missingKeywordResponse;
        }
        String signingSecret = System.getenv("SLACK_SIGNING_SECRET");

        if (signingSecret == null) {
            LOGGER.severe("SLACK_SIGNING_SECRET environment variable has not been configured");
            return errorResponse;
        }
        String apiKey = System.getenv("GIPHY_API_KEY");

        if (apiKey == null) {
            LOGGER.severe("GIPHY_API_KEY environment variable has not been configured");
            return errorResponse;
        }
        String slackTimestamp = request.getHeaders().get("x-slack-request-timestamp");

        String slackSigningBaseString = "v0:" + slackTimestamp + ":" + slackData;
        String slackSignature = request.getHeaders().get("x-slack-signature");

        boolean match = matchSignature(signingSecret, slackSigningBaseString, slackSignature);

        if (!match) {
            LOGGER.severe("Signature matching failed. Can be invoked only from Slack");
            return errorResponse;
        }

        LOGGER.info("Signature match successful");

        String giphyResponse = null;
        try {
            giphyResponse = getRandomGiphyImage(slackdataMap.get("text"), apiKey);
        } catch (Exception ex) {
            LOGGER.severe("Failed to fetch random image from using Giphy API - " + ex.getMessage());
            return errorResponse;
        }

        //MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GiphyRandomAPIGetResponse giphyModel = null;
        try {
            giphyModel = MAPPER.readValue(giphyResponse, GiphyRandomAPIGetResponse.class);
        } catch (Exception ex) {
            LOGGER.severe("Failed to marshall GIPHY API response - " + ex.getMessage());
            return errorResponse;
        }
        String title = giphyModel.getData().getTitle();
        String imageURL = giphyModel.getData().getImages().getDownsized().getUrl();

        LOGGER.info("Giphy image Title -- " + title);
        LOGGER.info("Giphy image URL -- " + imageURL);

        SlackSlashCommandResponse slackResponse = new SlackSlashCommandResponse();
        slackResponse.setText(SLACK_RESPONSE_STATIC_TEXT);

        Attachment attachment = new Attachment();
        attachment.setImageUrl(imageURL);
        attachment.setText(title);

        slackResponse.setAttachments(Arrays.asList(attachment));
        return request.createResponseBuilder(HttpStatus.OK).header("Content-type", "application/json").body(slackResponse).build();
    }

    /**
     * Fetches a random GIF from GIPHY
     *
     * @param searchTerm keyword sent along with the Slash command e.g. /azfuncy
     * cat
     * @param apiKey GIHPY API key
     * @return JSON response from the GIPHY Random API
     * @throws IOException
     */
    private static String getRandomGiphyImage(String searchTerm, String apiKey) throws IOException {
        String giphyResponse = null;
        if (HTTP_CLIENT == null) {
            HTTP_CLIENT = HttpClients.createDefault();
            LOGGER.info("Instantiated new HTTP client");
        }
        String giphyURL = "http://api.giphy.com/v1/gifs/random?tag=" + searchTerm + "&api_key=" + apiKey;
        LOGGER.info("Invoking GIPHY endpoint - " + giphyURL);

        HttpGet giphyGETRequest = new HttpGet(giphyURL);
        CloseableHttpResponse response = HTTP_CLIENT.execute(giphyGETRequest);

        giphyResponse = EntityUtils.toString(response.getEntity());

        return giphyResponse;
    }

    /**
     * Calculates the signature based on Slack data and matches it with Slack
     * signature in X-Slack-Signature header - as per
     * https://api.slack.com/docs/verifying-requests-from-slack#a_recipe_for_security
     *
     * @param signingSecret Slack app config - Signing Secret
     * @param slackSigningBaseString Slack data used for signing
     * @param slackSignature signature sent by Slack
     * @return true if signatures match, otherwise false
     */
    private static boolean matchSignature(String signingSecret, String slackSigningBaseString, String slackSignature) {
        boolean result;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(slackSigningBaseString.getBytes());

            String hexSignature = DatatypeConverter.printHexBinary(hash);
            result = ("v0=" + hexSignature.toLowerCase()).equals(slackSignature);
        } catch (Exception e) {
            LOGGER.severe("Signature matching issue " + e.getMessage());
            result = false;
        }
        return result;
    }
}
