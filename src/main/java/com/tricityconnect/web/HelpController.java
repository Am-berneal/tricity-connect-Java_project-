package com.tricityconnect.web;

import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.HelpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Help Centre with FAQs, the Transit Assistant chat (kept in the session) and grievances. */
@Controller
@RequestMapping("/help")
public class HelpController {

    public record ChatMessage(boolean fromUser, String text, LocalTime time) {
    }

    private final HelpService helpService;
    private final CurrentUser currentUser;

    public HelpController(HelpService helpService, CurrentUser currentUser) {
        this.helpService = helpService;
        this.currentUser = currentUser;
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> chat(HttpSession session) {
        Object o = session.getAttribute("chat");
        if (o == null) {
            List<ChatMessage> c = new ArrayList<>();
            c.add(new ChatMessage(false, "Namaste! Ask me about next buses, stops, fares, refunds, passes or ladies seats. "
                    + "Try: \"next bus from Kharar to Sector 17\".", LocalTime.now()));
            session.setAttribute("chat", c);
            return c;
        }
        return (List<ChatMessage>) o;
    }

    @GetMapping
    public String help(@RequestParam(defaultValue = "all") String topic, HttpSession session, Model model) {
        var faqs = helpService.faqs();
        model.addAttribute("faqs", topic.equals("all") ? faqs : faqs.stream().filter(f -> f.category().equals(topic)).toList());
        model.addAttribute("allFaqs", faqs);
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        HelpService.CATEGORIES.forEach(c -> counts.put(c, faqs.stream().filter(f -> f.category().equals(c)).count()));
        model.addAttribute("faqCounts", counts);
        model.addAttribute("topic", topic);
        model.addAttribute("categories", HelpService.CATEGORIES);
        model.addAttribute("chat", chat(session));
        return "help";
    }

    @PostMapping("/ask")
    public String ask(@RequestParam String q, HttpSession session) {
        List<ChatMessage> c = chat(session);
        c.add(new ChatMessage(true, q, LocalTime.now()));
        c.add(new ChatMessage(false, helpService.answer(q), LocalTime.now()));
        while (c.size() > 20) {
            c.remove(0);
        }
        return "redirect:/help#chat";
    }

    @PostMapping("/chat/clear")
    public String clear(HttpSession session) {
        session.removeAttribute("chat");
        return "redirect:/help#chat";
    }

    @PostMapping("/grievance")
    public String grievance(@RequestParam String category, @RequestParam(required = false) String pnr,
                            @RequestParam String message, RedirectAttributes flash) {
        try {
            var g = helpService.file(currentUser.require(), category, pnr, message);
            flash.addFlashAttribute("success", "Grievance filed. Your tracking ID is " + g.getTicketNo()
                    + ". The Chief Transit Officer (CTU) will reply within 2 working days.");
        } catch (BookingException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/help";
    }
}
