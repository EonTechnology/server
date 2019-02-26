package org.eontechology.and.eon.app.cfg;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ForkProperties {

    private long minDepositSize;
    private Period[] periods;

    public long getMinDepositSize() {
        return minDepositSize;
    }

    public void setMinDepositSize(long minDepositSize) {
        this.minDepositSize = minDepositSize;
    }

    public Period[] getPeriods() {
        return periods;
    }

    public void setPeriods(Period[] periods) {
        this.periods = periods;
    }

    public static class Period {
        private String dateEnd;
        private Integer number;
        private Map<String, String> added;
        private String[] removed;
        private String[] addedRules;
        private String[] removedRules;

        public String getDateEnd() {
            return dateEnd;
        }

        public void setDateEnd(String dateEnd) {
            this.dateEnd = dateEnd;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public Map<String, String> getAddedTxTypes() {
            return added;
        }

        public void setAddedTxTypes(Map<String, String> added) {
            this.added = added;
        }

        public String[] getRemovedTxTypes() {
            return removed;
        }

        public void setRemovedTxTypes(String[] removed) {
            this.removed = removed;
        }

        public String[] getAddedRules() {
            return addedRules;
        }

        public void setAddedRules(String[] addedRules) {
            this.addedRules = addedRules;
        }

        public String[] getRemovedRules() {
            return removedRules;
        }

        public void setRemovedRules(String[] removedRules) {
            this.removedRules = removedRules;
        }
    }

    public static ForkProperties parse(URI uri) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] json = Files.readAllBytes(Paths.get(uri));
        Map<String, Object> forksMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        ForkProperties props = new ForkProperties();
        props.setMinDepositSize(Long.parseLong(forksMap.get("min_deposit_size").toString()));

        List<Period> forksPeriods = new LinkedList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> periodJson = (List<Map<String, Object>>) forksMap.get("forks");
        for (Map<String, Object> map : periodJson) {

            ForkProperties.Period p = new ForkProperties.Period();

            p.setNumber((Integer) map.get("number"));
            p.setDateEnd(map.get("date_end").toString());

            if (map.containsKey("parser")) {

                @SuppressWarnings("unchecked")
                Map<String, Object> parserMap = (Map<String, Object>) map.get("parser");

                int fieldCount = 0;
                @SuppressWarnings("unchecked")
                Map<String, String> added = (Map<String, String>) parserMap.get("add");
                if (added != null) {
                    p.setAddedTxTypes(added);
                    fieldCount++;
                }

                @SuppressWarnings("unchecked")
                List<String> removed = (List<String>) parserMap.get("remove");
                if (removed != null) {
                    p.setRemovedTxTypes(removed.toArray(new String[0]));
                    fieldCount++;
                }

                if (fieldCount != parserMap.size()) {
                    throw new IOException("Invalid forks-file format");
                }
            }

            if (map.containsKey("validator")) {

                @SuppressWarnings("unchecked")
                Map<String, Object> validator = (Map<String, Object>) map.get("validator");
                int fieldCount = 0;

                @SuppressWarnings("unchecked")
                List<String> added = (List<String>) validator.get("add");
                if (added != null) {
                    p.setAddedRules(added.toArray(new String[0]));
                    fieldCount++;
                }

                @SuppressWarnings("unchecked")
                List<String> removed = (List<String>) validator.get("remove");
                if (removed != null) {
                    p.setRemovedRules(removed.toArray(new String[0]));
                    fieldCount++;
                }

                if (fieldCount != validator.size()) {
                    throw new IOException("Invalid forks-file format");
                }
            }

            forksPeriods.add(p);

            int size = 2;
            size += map.containsKey("parser") ? 1 : 0;
            size += map.containsKey("validator") ? 1 : 0;

            if (size != map.size()) {
                throw new IOException("Invalid forks-file format");
            }
        }

        props.setPeriods(forksPeriods.toArray(new ForkProperties.Period[0]));

        return props;
    }
}
