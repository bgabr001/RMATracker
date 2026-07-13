package com.harpenterprises.rmatracker.model;

public class RepairItem {
    private String county;
    private String machineType;
    private String serialNumber;
    private String version;
    private String problemDescription;

    public RepairItem(){
    }

    public RepairItem(String county,
                      String machineType,
                      String serialNumber,
                      String problemDescription,
                      String version) {
        this.county = county;
        this.machineType = machineType;
        this.serialNumber = serialNumber;
        this.problemDescription = problemDescription;
        this.version = version;
    }
    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    @Override
    public String toString() {
        return county + " | "
                + machineType + " | "
                + serialNumber + " | "
                + "Version:" + version;
    }
}

