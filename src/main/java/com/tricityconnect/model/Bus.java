package com.tricityconnect.model;

import jakarta.persistence.*;

@Entity
@Table(name = "buses")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Registration number, e.g. CH01-GA-2041 */
    @Column(nullable = false, unique = true, length = 20)
    private String regNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BusType type;

    /** Number of seats; must be a multiple of 4 (2+2 layout). */
    private int capacity;

    @Column(length = 60)
    private String depot;

    @Column(length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BusStatus status = BusStatus.ACTIVE;

    public Bus() {
    }

    public Bus(String regNo, BusType type, int capacity, String depot) {
        this.regNo = regNo;
        this.type = type;
        this.capacity = capacity;
        this.depot = depot;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public BusType getType() { return type; }
    public void setType(BusType type) { this.type = type; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDepot() { return depot; }
    public void setDepot(String depot) { this.depot = depot; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BusStatus getStatus() { return status; }
    public void setStatus(BusStatus status) { this.status = status; }
}
