package com.tricityconnect.model;

import jakarta.persistence.*;

/** A bus stop. City and state matter because the Tricity spans UT Chandigarh, Punjab and Haryana. */
@Entity
@Table(name = "stops")
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String city;

    @Column(nullable = false, length = 30)
    private String state;

    public Stop() {
    }

    public Stop(String name, String city, String state) {
        this.name = name;
        this.city = city;
        this.state = state;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
