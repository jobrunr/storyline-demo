package org.jobrunr.government;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ExpenseValidationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpenseValidationController.class);

    @GetMapping({"/verify"})
    @ResponseBody
    public String verify() {
        LOGGER.info("verifying credentials");
        return "looks good to me!";
    }

}
