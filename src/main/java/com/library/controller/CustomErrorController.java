package com.library.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        String errorTitle = "Error";
        String errorMsg = "An unexpected error occurred";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if (statusCode == 404) {
                errorTitle = "Page Not Found";
                errorMsg = "The page you are looking for does not exist.";
            } else if (statusCode == 500) {
                errorTitle = "Server Error";
                errorMsg = message != null ? message.toString() : "An internal server error occurred.";
            }
        } else if (message != null && !message.toString().isEmpty()) {
            errorMsg = message.toString();
        }

        model.addAttribute("error", errorTitle);
        model.addAttribute("message", errorMsg);
        return "error";
    }
}
