package org.prebid.pg.delstats.config;

import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public CsvMapperFactory csvMapperFactory() {
        return new CsvMapperFactory();
    }

    public static class CsvMapperFactory {
        private CsvMapper csvMapper;

        public CsvMapperFactory() {
            csvMapper = new CsvMapper();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            csvMapper.setDateFormat(dateFormat);
            csvMapper.enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING);
        }

        public CsvMapper getCsvMapper() {
            return csvMapper;
        }
    }

}

