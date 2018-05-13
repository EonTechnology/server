package com.exscudo.eon.app.cfg;

public class ForkProperties {

    private String dateEndAll;
    private long minDepositSize;
    private Period[] periods;

    public String getDateEndAll() {
        return dateEndAll;
    }

    public void setDateEndAll(String dateEndAll) {
        this.dateEndAll = dateEndAll;
    }

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
        private String dateBegin;
        private Integer number;
        private String[] addedTxTypes;
        private String[] removedTxTypes;

        public String getDateBegin() {
            return dateBegin;
        }

        public void setDateBegin(String dateBegin) {
            this.dateBegin = dateBegin;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

        public String[] getAddedTxTypes() {
            return addedTxTypes;
        }

        public void setAddedTxTypes(String[] addedTxTypes) {
            this.addedTxTypes = addedTxTypes;
        }

        public String[] getRemovedTxTypes() {
            return removedTxTypes;
        }

        public void setRemovedTxTypes(String[] removedTxTypes) {
            this.removedTxTypes = removedTxTypes;
        }
    }
}
