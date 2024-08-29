package com.example.movieAssistant.services.impl;

import com.example.movieAssistant.exceptions.CustomException;
import com.example.movieAssistant.model.db.entity.Authority;
import com.example.movieAssistant.model.db.entity.User;
import com.example.movieAssistant.model.db.entity.UserInfo;
import com.example.movieAssistant.model.db.repository.AuthorityRepo;
import com.example.movieAssistant.model.db.repository.UserInfoRepo;
import com.example.movieAssistant.model.db.repository.UserRepo;
import com.example.movieAssistant.model.dto.request.AuthorityRequest;
import com.example.movieAssistant.model.dto.request.PasswordChangeRequest;
import com.example.movieAssistant.model.dto.request.UserRequest;
import com.example.movieAssistant.model.dto.response.AuthorityResponse;
import com.example.movieAssistant.model.dto.response.JwtAuthenticationResponse;
import com.example.movieAssistant.model.dto.response.UserInfoResponse;
import com.example.movieAssistant.model.dto.response.UserResponse;
import com.example.movieAssistant.services.JWTService;
import com.example.movieAssistant.services.UserService;
import com.example.movieAssistant.utils.PaginationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final UserInfoRepo userInfoRepo;
    private final AuthorityRepo authorityRepo;
    private final PasswordEncoder encoder;
    private final JWTService jwtService;
    private final ObjectMapper mapper;

    @Transactional
    @Override
    public Page<UserResponse> getAllUsers(Integer page, Integer perPage, String sort, Sort.Direction order) {
        Pageable request = PaginationUtil.getPageRequest(page, perPage, sort, order);

        List<UserResponse> all = userRepo.findAll(request)
                .getContent()
                .stream()
                .map(user -> {
                    UserInfoResponse infoResponse = mapper.convertValue(user.getUserInfo(), UserInfoResponse.class);
                    infoResponse.setBirthDay(user.getUserInfo().getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

                    UserResponse response = mapper.convertValue(user, UserResponse.class);
                    response.setUserInfo(infoResponse);
                    return response;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(all);
    }

    @Transactional
    @Override
    public User getUser(String user) {
        if(user.isEmpty())
            throw new CustomException("Не указано имя пользователя", HttpStatus.BAD_REQUEST);

        Optional<User> userOptional = userRepo.findById(user);
        if(userOptional.isEmpty())
            throw new CustomException("Пользователь с таким логином отсутствует", HttpStatus.NOT_FOUND);

        return userOptional.get();
    }

    @Transactional
    @Override
    public UserResponse getUserResponse(String userName) {
        User user = getUser(userName);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        List<String> listAuthorities = authorities
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (!listAuthorities.contains("ROLE_ADMIN")) {
            if (!user.getUsername().equals(principal.getUsername())) {
                throw new CustomException("Пользователь не имеет прав на получение сведений о другом пользователе", HttpStatus.FORBIDDEN);
            }
        }

        UserInfoResponse infoResponse = mapper.convertValue(user.getUserInfo(), UserInfoResponse.class);
        infoResponse.setBirthDay(user.getUserInfo().getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        UserResponse response = mapper.convertValue(user, UserResponse.class);
        response.setUserInfo(infoResponse);
        return response;
    }

    @Transactional
    @Override
    public User getThisUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return getUser(userDetails.getUsername());
    }

    @Transactional
    @Override
    public JwtAuthenticationResponse createUser(UserRequest request) {

        userRepo.findById(request.getUsername())
                .ifPresent(user -> {
                    throw new CustomException("Данный логин уже существует", HttpStatus.CONFLICT);
                });
        userInfoRepo.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new CustomException("Данный email уже существует", HttpStatus.CONFLICT);
                });

        LocalDate bDay = null;

        if (request.getBirthDay() == null) {
            throw new CustomException("Дата рождения должна быть указана", HttpStatus.BAD_REQUEST);
        }
        else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            try {
                bDay = LocalDate.parse(request.getBirthDay(), formatter);
                if (bDay.isAfter(LocalDate.now().minusYears(18))) {
                    throw new CustomException("Пользователь не может быть младше 18 лет", HttpStatus.BAD_REQUEST);
                }
            } catch (DateTimeParseException e) {
                throw new CustomException("Некорректная дата рождения", HttpStatus.BAD_REQUEST);
            }
        }

        User user = new User(request.getUsername(), request.getPassword());

        String pass = user.getPassword();
        user.setPassword(encoder.encode(pass));

        user.setEnabled(true);

        userRepo.save(user);

        Authority auth = new Authority("ROLE_USER");
        user.addAuthority(auth);
        auth.setUsername(user);
        authorityRepo.save(auth);

        UserInfo userInfo = new UserInfo(request.getEmail(), user, request.getFirstName(), request.getLastName(), bDay, LocalDateTime.now());
        userInfo = userInfoRepo.save(userInfo);
        user.setUserInfo(userInfo);

        String jwt = jwtService.generateToken(request.getUsername());
        String jwtR = jwtService.generateRToken(request.getUsername());

        return new JwtAuthenticationResponse(jwt, jwtR);
    }

    @Transactional
    @Override
    public String updatePassword(PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        if(!principal.getUsername().equals(request.getUsername())) {
            throw new CustomException("Пользователь не имеет прав на редактирование пароля другого пользователя", HttpStatus.FORBIDDEN);
        }

        User user = getUser(request.getUsername());

        if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CustomException("Неверно указан старый пароль", HttpStatus.FORBIDDEN);
        }

        if (encoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException("В качестве нового пароля указан текущий", HttpStatus.FORBIDDEN);
        }

        String pass = request.getNewPassword();

        String codPass = encoder.encode(pass);
        user.setPassword(codPass);

        userRepo.save(user);

        return "Пароль пользователя " + user.getUsername() + " успешно изменен.";
    }

    @Transactional
    public AuthorityResponse updateAuthority (AuthorityRequest request){
        User user = getUser(request.getUsername());

        authorityRepo.deleteAllByUsername(user);
        authorityRepo.flush();
        user.setAuthorities(new ArrayList<>());

        for(String str: request.getAuthorities()){
            Authority auth = new Authority(str);
            user.addAuthority(auth);
            auth.setUsername(user);
            authorityRepo.save(auth);
        }

        User newUser = userRepo.findById(request.getUsername()).get();
        return new AuthorityResponse(newUser.getUsername(), newUser.getAuthorities());
    }

    @Transactional
    public AuthorityResponse addAuthority (String username, List <String> newAuthority){
        User user = getUser(username);
        List <String> userAuthorities = user.getAuthoritiesString();

        for(String str: newAuthority){
            if (!userAuthorities.contains(str)){
                Authority auth = new Authority(str);
                user.addAuthority(auth);
                auth.setUsername(user);
                authorityRepo.save(auth);
            }
        }

        user = userRepo.save(user);
        return new AuthorityResponse(user.getUsername(), user.getAuthorities());
    }

    @Transactional
    public AuthorityResponse  deleteAuthority (String username, List <String> newAuthority){
        User user = getUser(username);
        List<String> list = user.getAuthoritiesString();
        list.removeAll(newAuthority);

        authorityRepo.deleteAllByUsername(user);
        authorityRepo.flush();
        user.setAuthorities(new ArrayList<>());

        for(String str: list){
            Authority auth = new Authority(str);
            user.addAuthority(auth);
            auth.setUsername(user);
            authorityRepo.save(auth);
        }

        User newUser = userRepo.findById(user.getUsername()).get();
        return new AuthorityResponse(newUser.getUsername(), newUser.getAuthorities());
    }

    @Transactional
    public String setEnable(String username, boolean requestIsEnable) {
        User user = getUser(username);

        if (user.isEnabled()==requestIsEnable) {
            if (user.isEnabled()){
                return "User " + username + " already is enabled";
            } else
                return "User " + username + " already is disabled";
        } else {
            user.setEnabled(requestIsEnable);
            user.setTimeOfSwitchStatus(LocalDateTime.now());
            user = userRepo.save(user);

            if (user.isEnabled()){
                return "User " + username + " is enabled";
            } else
                return "User " + username + " is disabled";
        }
    }
}
