package com.tricityconnect.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/** Opens every page and checks it renders (catches template mistakes) and that security rules work. */
@SpringBootTest
@AutoConfigureMockMvc
class PagesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    com.tricityconnect.repository.StopRepository stopRepository;

    @Test
    void publicPagesOpenWithoutLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Busiest Express Routes")));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/alerts")).andExpect(status().isOk()).andExpect(content().string(containsString("Live Transit Bulletins")));
        mvc.perform(get("/search")).andExpect(status().isOk());
    }

    @Test
    void hindiAndPunjabiWork() throws Exception {
        mvc.perform(get("/").param("lang", "hi")).andExpect(content().string(containsString("सीट बुक करें")));
        mvc.perform(get("/").param("lang", "pa")).andExpect(content().string(containsString("ਸੀਟ ਬੁੱਕ ਕਰੋ")));
    }

    @Test
    void privatePagesNeedLogin() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/passes")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithUserDetails("9876543210")
    void passengerPagesRender() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().isOk()).andExpect(content().string(containsString("Active Commute Hub")));
        mvc.perform(get("/bookings").param("tab", "completed")).andExpect(status().isOk());
        mvc.perform(get("/bookings").param("tab", "cancelled")).andExpect(status().isOk());
        mvc.perform(get("/passes")).andExpect(status().isOk()).andExpect(content().string(containsString("My Commuter Passes")));
        mvc.perform(get("/passes/new")).andExpect(status().isOk()).andExpect(content().string(containsString("Get Your Monthly Commuter Pass")));
        mvc.perform(get("/passes/new").param("seat", "5C").param("months", "3")).andExpect(status().isOk());
        mvc.perform(get("/alerts")).andExpect(status().isOk()).andExpect(content().string(containsString("Subscribe to Route Alerts")));
    }

    @Test
    @WithUserDetails("9876543210")
    void searchSeatAndCheckoutPagesRender() throws Exception {
        String kharar = stopRepository.findByName("Kharar Bus Stand").orElseThrow().getId().toString();
        String sector17 = stopRepository.findByName("Sector 17 ISBT").orElseThrow().getId().toString();
        String html = mvc.perform(get("/search").param("from", kharar).param("to", sector17)
                        .param("date", java.time.LocalDate.now().plusDays(1).toString()).param("slot", "all"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // first "Select Seat" link on the page
        int i = html.indexOf("/trips/");
        if (i > 0) {
            String link = html.substring(i, html.indexOf('"', i)).replace("&amp;", "&");
            mvc.perform(get(link)).andExpect(status().isOk()).andExpect(content().string(containsString("Your Reservation")));
        }
    }

    @Test
    @WithUserDetails("admin@tricity.in")
    void adminPagesRender() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
        mvc.perform(get("/admin/routes")).andExpect(status().isOk());
        mvc.perform(get("/admin/buses")).andExpect(status().isOk());
        mvc.perform(get("/admin/trips")).andExpect(status().isOk());
        mvc.perform(get("/admin/disruptions")).andExpect(status().isOk()).andExpect(content().string(containsString("Create Disruption Advisory")));
        mvc.perform(get("/admin/fares")).andExpect(status().isOk()).andExpect(content().string(containsString("Fare Preview Calculator")));
        mvc.perform(get("/admin/fares").param("month", "2026-11").param("concession", "STUDENT").param("time", "18:00")).andExpect(status().isOk());
        mvc.perform(get("/admin/reports")).andExpect(status().isOk()).andExpect(content().string(containsString("Corridor ledger")));
        mvc.perform(get("/admin/reports/export.csv")).andExpect(status().isOk()).andExpect(content().string(containsString("Route,Corridor")));
        mvc.perform(get("/conductor")).andExpect(status().isOk()).andExpect(content().string(containsString("Select your trip")));
    }

    @Test
    @WithUserDetails("conductor@tricity.in")
    void conductorEtmRenders() throws Exception {
        String html = mvc.perform(get("/conductor")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int i = html.indexOf("/conductor?tripId=");
        if (i > 0) {
            String link = html.substring(i, html.indexOf('"', i)).replace("&amp;", "&");
            mvc.perform(get(link)).andExpect(status().isOk()).andExpect(content().string(containsString("Seat map")));
        }
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("9876543210")
    void profileAndHelpRender() throws Exception {
        for (String tab : new String[]{"info", "wallet", "routes", "contacts", "language"}) {
            mvc.perform(get("/profile").param("tab", tab)).andExpect(status().isOk());
        }
        mvc.perform(get("/help")).andExpect(status().isOk());
        mvc.perform(get("/alerts")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("9876543210")
    void passengerCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }
}
