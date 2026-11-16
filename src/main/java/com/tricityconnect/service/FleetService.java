package com.tricityconnect.service;

import com.tricityconnect.model.Bus;
import com.tricityconnect.model.BusStatus;
import com.tricityconnect.model.BusType;
import com.tricityconnect.model.Trip;
import com.tricityconnect.repository.BusRepository;
import com.tricityconnect.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Fleet (buses): add, change status, and which route each bus is running today. */
@Service
public class FleetService {

    private final BusRepository busRepository;
    private final TripRepository tripRepository;

    public FleetService(BusRepository busRepository, TripRepository tripRepository) {
        this.busRepository = busRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<Bus> all() {
        return busRepository.findAllByOrderByRegNoAsc();
    }

    /** Next (or current) trip of every bus today, for the "assigned corridor" column. */
    @Transactional(readOnly = true)
    public Map<Long, Trip> nextTripByBus() {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Trip> m = new HashMap<>();
        for (Trip t : tripRepository.findByDepartureTimeBetweenOrderByDepartureTime(
                now.minusHours(1), LocalDate.now().plusDays(1).atStartOfDay())) {
            m.putIfAbsent(t.getBus().getId(), t);
        }
        return m;
    }

    @Transactional
    public Bus add(String regNo, BusType type, int capacity, String model, String depot) {
        String reg = regNo == null ? "" : regNo.trim().toUpperCase();
        if (!reg.matches("[A-Z]{2}\\d{2}-[A-Z]{1,2}-\\d{3,4}")) {
            throw new BookingException("Registration must look like CH01-GA-2041.");
        }
        if (busRepository.existsByRegNoIgnoreCase(reg)) {
            throw new BookingException("Bus " + reg + " is already registered.");
        }
        if (capacity < 16 || capacity > 60 || capacity % 4 != 0) {
            throw new BookingException("Seats must be a multiple of 4 between 16 and 60 (2+2 layout).");
        }
        if (type == null) {
            throw new BookingException("Choose the powertrain type.");
        }
        Bus b = new Bus(reg, type, capacity, depot);
        b.setModel(model);
        b.setStatus(BusStatus.STANDBY);
        return busRepository.save(b);
    }

    @Transactional
    public Bus setStatus(Long id, BusStatus status) {
        Bus b = busRepository.findById(id).orElseThrow(() -> new NotFoundException("Bus not found"));
        b.setStatus(status);
        return b;
    }
}
