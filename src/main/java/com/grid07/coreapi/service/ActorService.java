package com.grid07.coreapi.service;

import com.grid07.coreapi.dto.BotResponse;
import com.grid07.coreapi.dto.CreateBotRequest;
import com.grid07.coreapi.dto.CreateUserRequest;
import com.grid07.coreapi.dto.UserResponse;
import com.grid07.coreapi.model.AppUser;
import com.grid07.coreapi.model.Bot;
import com.grid07.coreapi.repository.BotRepository;
import com.grid07.coreapi.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActorService {

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    public ActorService(UserRepository userRepository, BotRepository botRepository) {
        this.userRepository = userRepository;
        this.botRepository = botRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        AppUser user = userRepository.saveAndFlush(new AppUser(request.username(), request.premium()));
        return UserResponse.from(user);
    }

    @Transactional
    public BotResponse createBot(CreateBotRequest request) {
        Bot bot = botRepository.saveAndFlush(new Bot(request.name(), request.personaDescription()));
        return BotResponse.from(bot);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BotResponse> listBots() {
        return botRepository.findAll()
                .stream()
                .map(BotResponse::from)
                .toList();
    }
}
