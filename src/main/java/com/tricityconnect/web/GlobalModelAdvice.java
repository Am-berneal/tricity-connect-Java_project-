package com.tricityconnect.web;

import com.tricityconnect.model.AppUser;
import com.tricityconnect.repository.AlertRepository;
import com.tricityconnect.service.BookingException;
import com.tricityconnect.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/** Adds the logged-in user and the alert count to every page (used by the navbar). */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUser currentUser;
    private final AlertRepository alertRepository;

    public GlobalModelAdvice(CurrentUser currentUser, AlertRepository alertRepository) {
        this.currentUser = currentUser;
        this.alertRepository = alertRepository;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return currentUser.get().orElse(null);
    }

    @ModelAttribute("alertCount")
    public long alertCount() {
        return alertRepository.findByActiveTrueOrderByPriorityAscCreatedAtDesc().size();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(NotFoundException e) {
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("status", 404);
        mv.addObject("message", e.getMessage());
        mv.addObject("currentUser", currentUser());
        mv.addObject("alertCount", alertCount());
        return mv;
    }

    @ExceptionHandler(BookingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView ruleBroken(BookingException e) {
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("status", 400);
        mv.addObject("message", e.getMessage());
        mv.addObject("currentUser", currentUser());
        mv.addObject("alertCount", alertCount());
        return mv;
    }
}
