package com.mnebot.riptide.domain.model

enum class MarineCategory(val isUnlockedByDefault: Boolean) {
    FISH(true),
    FLORA(true),
    CRUSTACEAN(true),
    MOLLUSK(true),
    PELAGIC(true),
    CEPHALOPOD(false),
    REPTILE(false),
    MAMMAL(false),
    DECORATION(false)
}