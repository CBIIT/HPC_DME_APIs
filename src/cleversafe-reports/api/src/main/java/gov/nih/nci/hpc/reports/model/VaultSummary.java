package gov.nih.nci.hpc.reports.model;

public class VaultSummary {

    private String name = null;
    private String description = null;
    private Long capacity = null;
    private Long used = null;

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

    public Long getCapacity() {
        return capacity;
    }
    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public Long getUsed() {
        return used;
    }
    public void setUsed(Long used) {
        this.used =  used;
    }

}
