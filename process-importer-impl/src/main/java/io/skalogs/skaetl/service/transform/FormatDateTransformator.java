package io.skalogs.skaetl.service.transform;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.skalogs.skaetl.domain.ParameterTransformation;
import io.skalogs.skaetl.domain.TypeValidation;
import io.skalogs.skaetl.service.TransformatorProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class FormatDateTransformator extends TransformatorProcess {

    public FormatDateTransformator(TypeValidation type) {
        super(type);
    }

    public void apply(String idProcess, ParameterTransformation parameterTransformation, ObjectNode jsonValue, String value) {
        String valueToFormat = jsonValue.path(parameterTransformation.getFormatDateValue().getKeyField()).asText();
        if (StringUtils.isNotBlank(valueToFormat)) {
            try {
                SimpleDateFormat simpleDateFormatSource = new SimpleDateFormat(parameterTransformation.getFormatDateValue().getSrcFormat());
                Date date = simpleDateFormatSource.parse(valueToFormat);
                SimpleDateFormat simpleDateFormatTarget = new SimpleDateFormat(parameterTransformation.getFormatDateValue().getTargetFormat());
                String result = simpleDateFormatTarget.format(date);
                jsonValue.put(parameterTransformation.getFormatDateValue().getKeyField(), result);
            } catch (ParseException e) {
                log.error("ParseException on field {} for value {}", parameterTransformation.getFormatDateValue(), value);
            }
        }
    }
}
