package gov.nih.nci.hpc.reports.model;

public class VaultSummary {

    private Long id = 10L;
    private String name = "vault";
    private String description = "desc";
    private Long capacity = 1234567L;
    private Long used = 987L;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCapacity() {
        return capacity;
    }

    public Long getUsed() {
        return used;
    }

    public Long getId() {
        return id;
    }


}
