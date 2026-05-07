package com.grid07.coreapi.web;

import com.grid07.coreapi.dto.BotResponse;
import com.grid07.coreapi.dto.CreateBotRequest;
import com.grid07.coreapi.dto.CreateUserRequest;
import com.grid07.coreapi.dto.UserResponse;
import com.grid07.coreapi.service.ActorService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActorController {

    private final ActorService actorService;

    public ActorController(ActorService actorService) {
        this.actorService = actorService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = actorService.createUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return actorService.listUsers();
    }

    @PostMapping("/bots")
    public ResponseEntity<BotResponse> createBot(@Valid @RequestBody CreateBotRequest request) {
        BotResponse bot = actorService.createBot(request);
        return ResponseEntity.created(URI.create("/api/bots/" + bot.id())).body(bot);
    }

    @GetMapping("/bots")
    public List<BotResponse> listBots() {
        return actorService.listBots();
    }
}
