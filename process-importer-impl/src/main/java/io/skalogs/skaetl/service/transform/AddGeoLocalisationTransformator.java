package io.skalogs.skaetl.service.transform;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import io.skalogs.skaetl.domain.ParameterTransformation;
import io.skalogs.skaetl.domain.TypeValidation;
import io.skalogs.skaetl.service.TransformatorProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

@Slf4j
public class AddGeoLocalisationTransformator extends TransformatorProcess {

    private DatabaseReader reader;

    public AddGeoLocalisationTransformator(TypeValidation type) {
        super(type);

        // TODO: Replace this bullshit code
        try {
            File tmpFile = File.createTempFile("bwx", "dat");
            tmpFile.deleteOnExit();
            InputStream is = AddGeoLocalisationTransformator.class.getResourceAsStream("/GeoLite2-City.mmdb");
            OutputStream os = new FileOutputStream(tmpFile);

            byte[] buffer = new byte[4000];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            is.close();
            os.close();

            reader = new DatabaseReader.Builder(tmpFile).withCache(new CHMCache()).build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void apply(String idProcess, ParameterTransformation parameterTransformation, ObjectNode jsonValue, String value) {

        String key = parameterTransformation.getKeyField();
        String ipToResolve = jsonValue.path(parameterTransformation.getKeyField()).asText();

        log.debug("Start to localise IP address [{}]", ipToResolve);

        if (StringUtils.isNotBlank(ipToResolve)) {

            try {

                InetAddress ipAddress = InetAddress.getByName(ipToResolve);

                CityResponse response = reader.city(ipAddress);
                Country country = response.getCountry();
                Subdivision subdivision = response.getMostSpecificSubdivision();
                City city = response.getCity();
                Postal postal = response.getPostal();
                Location location = response.getLocation();

                jsonValue.
                        put(key + "_country_name", country.getName()).
                        put(key + "_country_isocode", country.getIsoCode()).
                        put(key + "_city_name", response.getCity().getName()).
                        put(key + "_subdivision_name", subdivision.getName()).
                        put(key + "_subdivision_isocode", subdivision.getIsoCode()).
                        put(key + "_city_name", city.getName()).
                        put(key + "_city_postalcode", postal.getCode()).
                        put(key + "_location_gp", location.getLatitude().toString() + "," + location.getLongitude().toString());

            } catch (AddressNotFoundException ex) {
                //
            } catch (Exception ex) {
                log.error("Exception during Geo IP Transformation");
                ex.printStackTrace();
            }
        }

        log.debug("End to localise IP address [{}]", ipToResolve);
    }
}