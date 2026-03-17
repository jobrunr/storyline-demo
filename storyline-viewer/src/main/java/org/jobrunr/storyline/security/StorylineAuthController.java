package org.jobrunr.storyline.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class StorylineAuthController {

    private final StorylineUserRepository userRepository;
    private final StorylineMagicLinkService magicLinkService;

    public StorylineAuthController(StorylineUserRepository userRepository, StorylineMagicLinkService magicLinkService) {
        this.userRepository = userRepository;
        this.magicLinkService = magicLinkService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/login/ott")
    public String ottVerifyPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token != null ? token : "");
        return "auth/verify-token";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam @Email @NotBlank String email,
            HttpServletRequest request, Model model) {
        if (!userRepository.existsByEmail(email)) {
            model.addAttribute("email", email);
            return "auth/register";
        }
        magicLinkService.sendMagicLink(request, email);
        model.addAttribute("email", email);
        return "auth/magic-link-sent";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam @NotBlank String name,
            @RequestParam(required = false) String company,
            @RequestParam @Email @NotBlank String email,
            HttpServletRequest request, Model model) {
        if (userRepository.existsByEmail(email)) {
            magicLinkService.sendMagicLink(request, email);
        } else {
            userRepository.save(StorylineUser.newUser(email, name, company));
            magicLinkService.sendMagicLink(request, email);
        }
        model.addAttribute("email", email);
        return "auth/magic-link-sent";
    }

}
