package com.example.movieAssistant.controllers;

import com.example.movieAssistant.model.dto.response.FriendshipResponse;
import com.example.movieAssistant.model.enums.RequestConfirmationStatus;
import com.example.movieAssistant.services.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friendships")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Tag(name="Отношения между пользователями")
public class FriendController {

    FriendService friendService;

    @PostMapping("/requests/{user-id}")
    @Operation(summary = "Направление заявки на добавление в друзья указанному пользователю")
    public FriendshipResponse sendRequest(@PathVariable("user-id") Long userId){
        return friendService.sendRequest(userId);
    }

    @PutMapping("/requests/consider/{friendship-id}")
    @Operation(summary = "Рассмотрение заявки в друзья по ее id")
    public FriendshipResponse considerRequest(@PathVariable("friendship-id") Long friendshipId, @RequestParam(defaultValue = "ACCEPTED") RequestConfirmationStatus status){
        return friendService.considerRequest(friendshipId, status);
    }

    @PutMapping("/status/{user-id}")
    @Operation(summary = "Изменение статуса отношений между текущим пользователем и указанным")
    public FriendshipResponse alterStatus(@PathVariable("user-id") Long userId, @RequestParam(defaultValue = "true") boolean status){
        return friendService.alterStatus(userId, status);
    }

    @GetMapping("/friends/{user-id}")
    @Operation(summary = "Получение информации о друзьях указанного пользователя")
    public Page<FriendshipResponse> getUsersAllFriends(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer perPage,
                                                       @RequestParam(defaultValue = "id") String sort,
                                                       @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                                       @PathVariable("user-id") Long userId)
    {
        return friendService.getAllUsersFriends(page, perPage, sort, order, userId);
    }

    @GetMapping("/friends")
    @Operation(summary = "Получение информации о друзьях текущего пользователя")
    public Page<FriendshipResponse> getMyAllFriends(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer perPage,
                                @RequestParam(defaultValue = "id") String sort,
                                @RequestParam(defaultValue = "ASC") Sort.Direction order) {
        return friendService.getMyAllFriends(page, perPage, sort, order);
    }

    @GetMapping("/requests/outgoing")
    @Operation(summary = "Получение информации об исходящих заявках текущего пользователя")
    public Page<FriendshipResponse> getMyOutgoingRequests(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer perPage,
                                      @RequestParam(defaultValue = "id") String sort,
                                      @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                      @RequestParam(defaultValue = "false") boolean withdrawn,
                                      @RequestParam(defaultValue = "UNCONSIDERED") RequestConfirmationStatus status)
    {
        return friendService.getMyOutgoingRequests(page, perPage, sort, order, withdrawn, status);
    }

    @GetMapping("/requests/outgoing/{user-id}")
    @Operation(summary = "Получение информации об исходящих заявках указанного пользователя")
    public Page<FriendshipResponse> getUsersOutgoingRequests(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer perPage,
                                                          @RequestParam(defaultValue = "id") String sort,
                                                          @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                                          @PathVariable("user-id") Long userId,
                                                          @RequestParam(defaultValue = "false") boolean withdrawn,
                                                          @RequestParam(defaultValue = "UNCONSIDERED") RequestConfirmationStatus status)
    {
        return friendService.getUsersOutgoingRequests(page, perPage, sort, order, userId, withdrawn, status);
    }

    @GetMapping("/requests/incoming")
    @Operation(summary = "Получение информации о входящих заявках текущего пользователя")
    public Page<FriendshipResponse> getMyIncomingRequests(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer perPage,
                                      @RequestParam(defaultValue = "id") String sort,
                                      @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                      @RequestParam(defaultValue = "false") boolean withdrawn,
                                      @RequestParam(defaultValue = "UNCONSIDERED") RequestConfirmationStatus status)
    {
        return friendService.getMyIncomingRequests(page, perPage, sort, order, withdrawn, status);
    }

    @GetMapping("/requests/incoming/{user-id}")
    @Operation(summary = "Получение информации о входящих заявках указанного пользователя")
    public Page<FriendshipResponse>  getUsersIncomingRequests(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer perPage,
                                         @RequestParam(defaultValue = "id") String sort,
                                         @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                         @PathVariable("user-id") Long userId,
                                         @RequestParam(defaultValue = "false") boolean withdrawn,
                                         @RequestParam(defaultValue = "UNCONSIDERED") RequestConfirmationStatus status)
    {
        return friendService.getUsersIncomingRequests(page, perPage, sort, order, userId, withdrawn, status);
    }

    @GetMapping("/status/{user-id}")
    @Operation(summary = "Получение информации о статусе отношений текущего пользователя с другим")
    public FriendshipResponse getStatusWithUser(@PathVariable("user-id") Long userId){
        return friendService.getStatusWithUser(userId);
    }

    @GetMapping("/status/{user1-id}/{user2-id}")
    @Operation(summary = "Получение информации о статусе отношений между указанными пользователями")
    public FriendshipResponse getStatusBetweenUsers(@PathVariable("user1-id") Long user1Id, @PathVariable("user2-id") Long user2Id){
        return friendService.getStatusBetweenUsers(user1Id, user2Id);
    }

    @GetMapping("/all")
    @Operation(summary = "Получение списка всех отношений пользователей в приложении")
    public Page<FriendshipResponse> getAllFriendships (@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer perPage,
                                      @RequestParam(defaultValue = "id") String sort,
                                      @RequestParam(defaultValue = "ASC") Sort.Direction order,
                                      @RequestParam(defaultValue = "false") boolean withdrawn,
                                      @RequestParam(defaultValue = "ACCEPTED") RequestConfirmationStatus status)
    {
        return friendService.getAllFriendships(page, perPage, sort, order, withdrawn, status);
    }
}
