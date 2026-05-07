package com.grid07.coreapi.config;

import com.grid07.coreapi.model.AppUser;
import com.grid07.coreapi.model.Bot;
import com.grid07.coreapi.repository.BotRepository;
import com.grid07.coreapi.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final boolean seedDemoData;
    private final UserRepository userRepository;
    private final BotRepository botRepository;

    public DemoDataSeeder(
            @Value("${app.seed-demo-data:true}") boolean seedDemoData,
            UserRepository userRepository,
            BotRepository botRepository
    ) {
        this.seedDemoData = seedDemoData;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
    }

    @Override
    public void run(String... args) {
        if (!seedDemoData) {
            return;
        }

        seedUsers();
        seedBotsForSpamTest();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.saveAll(List.of(
                new AppUser("alice", false),
                new AppUser("rohan", true)
        ));
    }

    private void seedBotsForSpamTest() {
        if (botRepository.count() >= 200) {
            return;
        }

        List<Bot> bots = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            String name = "bot-" + i;
            if (botRepository.findByName(name).isEmpty()) {
                bots.add(new Bot(name, "Demo bot " + i + " for concurrency testing"));
            }
        }
        botRepository.saveAll(bots);
    }
}
