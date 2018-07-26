package gov.nih.nci.hpc.reports.model;

public class VaultSummary {

    private String name = null;
    private String description = null;
    private Double capacity = null;
    private Double used = null;
    private String creationDate = null;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Double getCapacity() {
        return capacity;
    }
    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Double getUsed() {
        return used;
    }
    public void setUsed(Double used) {
        this.used =  used;
    }

    public String getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

}
