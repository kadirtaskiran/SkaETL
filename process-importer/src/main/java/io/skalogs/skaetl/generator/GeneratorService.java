package io.skalogs.skaetl.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import io.skalogs.skaetl.config.KafkaConfiguration;
import io.skalogs.skaetl.service.transform.AddGeoLocalisationTransformator;
import io.skalogs.skaetl.utils.KafkaUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GeneratorService {

    private final Producer<String, String> producer;
    private final String topic;
    private final ObjectMapper mapper = new ObjectMapper();
    private Random RANDOM = new Random();

    private final String[] tabDb = new String[]{
            "Oracle 11g",
            "Mysql 5.7.21"
    };
    private final String[] tabIp = new String[]{
            "10.14.15.1",
            "10.14.15.2",
            "10.14.15.3",
            "10.14.15.4",
            "10.121.120.41",
            "10.121.120.54",
            "10.121.120.64",
            "10.121.120.84"
    };
    private final String[] tabSrcIp = new String[]{
            "15.14.15.1",
            "15.14.15.2",
            "15.14.15.3"
    };
    private final String[] tabDbIp = new String[]{
            "171.14.15.1",
            "171.14.15.2"
    };

    public Date addMinutesAndSecondsToTime(int minutesToAdd, int secondsToAdd, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        cal.add(Calendar.MINUTE, minutesToAdd);
        cal.add(Calendar.SECOND, secondsToAdd);
        return cal.getTime();
    }

    public void createRandomNetwork(Integer nbElem) {
        for (int i = 0; i < nbElem; i++) {
            ISO8601DateFormat df = new ISO8601DateFormat();
            Date newDate = addMinutesAndSecondsToTime(i, RANDOM.nextInt(50), new Date());
            if (i % 2 == 0) {
                sendToKafka(RawNetworkDataGen.builder()
                        .timestamp(df.format(newDate))
                        .type("network")
                        .project("infra")
                        .messageSend(" Communication between server for timestamp" + df.format(newDate) + " for " + i)
                        .srcIp(tabIp[RANDOM.nextInt(tabIp.length)])
                        .destIp(tabIp[RANDOM.nextInt(tabIp.length)])
                        .osServer("RHEL 7.2")
                        .build());
            }
            if (i % 2 != 0) {
                sendToKafka(RawNetworkDataGen.builder()
                        .timestamp(df.format(newDate))
                        .type("network")
                        .project("infra")
                        .messageSend(" Communication between server for timestamp" + df.format(newDate) + " for " + i)
                        .srcIp(tabSrcIp[RANDOM.nextInt(tabSrcIp.length)])
                        .databaseIp(tabDbIp[RANDOM.nextInt(tabDbIp.length)])
                        .typeDatabase(tabDb[RANDOM.nextInt(tabDb.length)])
                        .osServer("RHEL 7.2")
                        .build());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void createRandom(Integer nbElemBySlot, Integer nbSlot) {

        for (int i = 0; i < nbSlot; i++) {
            for (int j = 0; j < nbElemBySlot; j++) {
                ISO8601DateFormat df = new ISO8601DateFormat();
                Date newDate = addMinutesAndSecondsToTime(i, RANDOM.nextInt(50), new Date());
                log.debug(i + "--" + j + "***" + df.format(newDate));
                sendToKafka(RawDataGen.builder()
                        .timestamp(df.format(newDate))
                        .type("gnii")
                        .project("toto")
                        .messageSend(" message number " + i + "--" + j + " for timestamp" + df.format(newDate))
                        .fieldTestToDelete("GNIIIIII")
                        .fieldTestToRename("Message to rename")
                        .build());
            }
        }
    }

    public void createApacheAsText(Integer nbElemBySlot, Integer nbSlot) {

        try {

            InputStream accessIS = AddGeoLocalisationTransformator.class.getResourceAsStream("/access.log");
            BufferedReader in = new BufferedReader(new InputStreamReader(accessIS));

            String line;
            int i = 0;
            ISO8601DateFormat df = new ISO8601DateFormat();

            while ((line = in.readLine()) != null) {

                Date newDate = addMinutesAndSecondsToTime(i, RANDOM.nextInt(50), new Date());

                sendToKafka(RawApacheTextDataGen.builder()
                        .type("apache_text")
                        .project("genere-apache-log")
                        .timestamp(df.format(newDate))
                        .message(line)
                        .build());

                if (i++ > (nbSlot * nbElemBySlot))
                    break;

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception ex) {
            log.error("Exception generating Apache log ", ex.getMessage());
        }
    }

    public void createApacheAsJSON(Integer nbElemBySlot, Integer nbSlot) {

        try {

            InputStream patternIS = AddGeoLocalisationTransformator.class.getResourceAsStream("/patterns");
            BufferedReader patterns = new BufferedReader(new InputStreamReader(patternIS));

            GrokCompiler grokInstance = GrokCompiler.newInstance();
            grokInstance.register(patterns);
            Grok grok = grokInstance.compile("%{COMMONAPACHELOG}");

            InputStream accessIS = AddGeoLocalisationTransformator.class.getResourceAsStream("/access.log");
            BufferedReader in = new BufferedReader(new InputStreamReader(accessIS));

            String line;
            int i = 0;
            ISO8601DateFormat df = new ISO8601DateFormat();

            while ((line = in.readLine()) != null) {


                Match match = grok.match(line);
                final java.util.Map<String, Object> capture =  match.capture();

                Date newDate = addMinutesAndSecondsToTime(i, RANDOM.nextInt(50), new Date());

                sendToKafka(RawApacheDataGen.builder()
                        .type("apache_json")
                        .project("genere-apache-log")
                        .timestamp(df.format(newDate))
                        .request(capture.get("request") == null ? "" : capture.get("request").toString())
                        .auth(capture.get("auth") == null ? "" : capture.get("auth").toString())
                        .bytes(capture.get("bytes") == null ? "" : capture.get("bytes").toString())
                        .clientip(capture.get("clientip") == null ? "" : capture.get("clientip").toString())
                        .httpversion(capture.get("httpversion") == null ? "" : capture.get("httpversion").toString())
                        .response(capture.get("response") == null ? "" : capture.get("response").toString())
                        .verb(capture.get("verb") == null ? "" : capture.get("verb").toString())
                        .build());

                if (i++ >= (nbSlot * nbElemBySlot))
                    break;

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception ex) {
            log.error("Exception generating Apache log ", ex.getMessage());
        }
    }

    private void sendToKafka(RawDataGen rdg) {
        try {
            String value = mapper.writeValueAsString(rdg);
            log.info("Sending {}", value);
            producer.send(new ProducerRecord(topic, value));
        } catch (Exception e) {
            log.error("Error sending to Kafka during generation ", e);
        }
    }

    private void sendToKafka(RawNetworkDataGen rdg) {
        try {
            String value = mapper.writeValueAsString(rdg);
            log.info("Sending {}", value);
            producer.send(new ProducerRecord(topic, value));
        } catch (Exception e) {
            log.error("Error sending to Kafka during generation ", e);
        }
    }

    private void sendToKafka(RawApacheDataGen ndg) {
        try {
            String value = mapper.writeValueAsString(ndg);
            log.info("Sending {}", value);
            producer.send(new ProducerRecord(topic, value));
        } catch (Exception e) {
            log.error("Error sending to Kafka during generation ", e);
        }
    }

    private void sendToKafka(RawApacheTextDataGen ndg) {
        try {
            String value = mapper.writeValueAsString(ndg);
            log.info("Sending {}", value);
            producer.send(new ProducerRecord(topic, value));
        } catch (Exception e) {
            log.error("Error sending to Kafka during generation ", e);
        }
    }

    public GeneratorService(KafkaConfiguration kafkaConfiguration, KafkaUtils kafkaUtils) {
        producer = kafkaUtils.kafkaProducer();
        topic = kafkaConfiguration.getTopic();
    }
}
