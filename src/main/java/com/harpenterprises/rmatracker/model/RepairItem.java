package com.harpenterprises.rmatracker.model;

public class RepairItem {
    private String county;
    private String machineType;
    private String serialNumber;
    private String version;
    private String problemDescription;
    private String repairDescription;
    private boolean received;

    public RepairItem() {
    }

    public RepairItem(
            String county,
            String machineType,
            String serialNumber,
            String version,
            String problemDescription,
            String repairDescription
    ) {
        this(county, machineType, serialNumber, version,
                problemDescription, repairDescription, false);
    }

    public RepairItem(
            String county,
            String machineType,
            String serialNumber,
            String version,
            String problemDescription,
            String repairDescription,
            boolean received
    ) {
        this.county = county;
        this.machineType = machineType;
        this.serialNumber = serialNumber;
        this.version = version;
        this.problemDescription = problemDescription;
        this.repairDescription = repairDescription;
        this.received = received;
    }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    public String getRepairDescription() { return repairDescription; }
    public void setRepairDescription(String repairDescription) { this.repairDescription = repairDescription; }
    public boolean isReceived() { return received; }
    public void setReceived(boolean received) { this.received = received; }

    @Override
    public String toString() {
        return county + " | " + machineType + " | " + serialNumber
                + " | Version: " + version
                + " | Received: " + (received ? "Yes" : "No");
    }
}
