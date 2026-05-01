package com.hangplan.security;

import com.hangplan.entity.AuthProvider;
import com.hangplan.entity.User;
import com.hangplan.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Expected OAuth2 user");
        }
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google account has no email");
        }
        String rawName = oauth2User.getAttribute("name");
        if (rawName == null || rawName.isBlank()) {
            rawName = oauth2User.getAttribute("given_name");
        }
        final String displayName = (rawName == null || rawName.isBlank()) ? email : rawName;
        String emailNorm = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(emailNorm)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(displayName)
                        .email(emailNorm)
                        .password(null)
                        .provider(AuthProvider.GOOGLE)
                        .build()));
        String token = jwtService.createToken(user);
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        String url = frontendUrl.replaceAll("/+$", "") + "/?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, url);
    }
}
