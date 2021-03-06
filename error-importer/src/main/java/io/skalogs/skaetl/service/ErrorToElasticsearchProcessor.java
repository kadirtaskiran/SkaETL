package io.skalogs.skaetl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import io.skalogs.skaetl.domain.ESBuffer;
import io.skalogs.skaetl.domain.ErrorData;
import io.skalogs.skaetl.domain.RetentionLevel;
import io.skalogs.skaetl.service.processor.AbstractElasticsearchProcessor;
import io.skalogs.skaetl.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.slf4j.MDC;

import java.text.ParseException;

@Slf4j
public class ErrorToElasticsearchProcessor extends AbstractElasticsearchProcessor<String, ErrorData> {

    private static final String NO_PROJECT = "no-project";
    private static final String ERRORS = "errors";
    private final ISO8601DateFormat df = new ISO8601DateFormat();

    public ErrorToElasticsearchProcessor(ESBuffer esBuffer, ESErrorRetryWriter esErrorRetryWriter) {
        super(esBuffer, esErrorRetryWriter);
    }

    @Override
    public void process(String key, ErrorData errorData) {
        try {
            String valueAsString = JSONUtils.getInstance().asJsonString(errorData);
            processToElasticsearch(df.parse(errorData.timestamp), NO_PROJECT, ERRORS, RetentionLevel.week, valueAsString);
        } catch (JsonProcessingException e) {
            log.error("Couldn't transform value " + errorData, e);
        } catch (ParseException e) {
            log.error("Couldn't parse date " + errorData, e);
        }
    }


    @Override
    protected void parseResultErrors(BulkResponse bulkItemResponses) {
        for (BulkItemResponse bir : bulkItemResponses) {
            MDC.put("item_error", bir.getFailureMessage());
            log.info("EsError" + bir.getFailureMessage());
            MDC.remove("item_error");
            //TODO ...
        }
    }
}
