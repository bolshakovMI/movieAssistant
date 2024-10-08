package com.example.movieAssistant.services.impl;

import com.example.movieAssistant.exceptions.CustomException;
import com.example.movieAssistant.model.db.entity.User;
import com.example.movieAssistant.model.db.repository.UserRepo;
import com.example.movieAssistant.model.dto.request.LoginRequest;
import com.example.movieAssistant.model.dto.response.JwtAuthenticationResponse;
import com.example.movieAssistant.services.AuthenticationService;
import com.example.movieAssistant.services.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepo repository;

    public JwtAuthenticationResponse signIn(LoginRequest request) throws CustomException {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));
        }
        catch (BadCredentialsException e) {
            throw new CustomException("Неверное имя пользователя или пароль", HttpStatus.BAD_REQUEST);
        }
        String jwt = jwtService.generateToken(request.getUsername());
        String jwtR = jwtService.generateRToken(request.getUsername());

        return new JwtAuthenticationResponse(jwt, jwtR);
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(HttpServletRequest request) throws CustomException {
        String authHeader = request.getHeader("Authorization");  // Получаем значение из заголовка запроса

        if (authHeader==null || authHeader.length() < 7) {
            throw new CustomException("Отсутствует bearer токен", HttpStatus.BAD_REQUEST);
        }
        String token = authHeader.substring("Bearer ".length());

        String scope = jwtService.extractScope(token);
        if (!scope.equals("refresh")) {
            throw new CustomException("Не refresh-токен", HttpStatus.BAD_REQUEST);
        }

        String username = jwtService.extractUserName(token);
        if (!repository.existsById(username)) {
            throw new CustomException("Держатель токена не зарегистрирован в БД", HttpStatus.BAD_REQUEST);
        }

        User user = repository.findById(username).get();
        String RToken = user.getToken();

        if(!token.equals(RToken)){
            throw new CustomException("Токен не зарегистрирован в БД", HttpStatus.BAD_REQUEST);
        }

        if (jwtService.isTokenExpired(token)) {
            throw new CustomException("Срок действия токена истек", HttpStatus.BAD_REQUEST);
        }

        String jwt = jwtService.generateToken(username);
        String jwtR = jwtService.generateRToken(username);

        return new JwtAuthenticationResponse(jwt, jwtR);
    }
}



