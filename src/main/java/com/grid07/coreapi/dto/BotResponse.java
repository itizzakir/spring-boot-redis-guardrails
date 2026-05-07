package com.grid07.coreapi.dto;

import com.grid07.coreapi.model.Bot;

public record BotResponse(
        Long id,
        String name,
        String personaDescription
) {
    public static BotResponse from(Bot bot) {
        return new BotResponse(bot.getId(), bot.getName(), bot.getPersonaDescription());
    }
}
