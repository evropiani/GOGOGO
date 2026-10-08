package com.gogogo.game.game;

import com.gogogo.game.engine.Rng;

import java.util.HashSet;

/** Silly generated opponent names. */
public final class BotNames {
    private BotNames() {}

    private static final String[] ADJ = {
            "Turbo", "Sneaky", "Wobbly", "Captain", "Sir", "Lil", "Big", "Mighty", "Dizzy", "Grumpy",
            "Fancy", "Sweaty", "Crispy", "Funky", "Soggy", "Spicy", "Cheeky", "Bouncy", "Rusty", "Zippy",
            "Silly", "Mega", "Tiny", "Salty", "Sleepy", "Loud", "Shiny", "Lady", "Doctor", "Professor",
            "Chunky", "Wonky", "Jolly", "Nervous", "Sticky", "Fluffy", "Speedy", "Clumsy", "Smol", "Hyper"};
    private static final String[] NOUN = {
            "Pickle", "Potato", "Noodle", "Waffle", "Muffin", "Bean", "Taco", "Nugget", "Pretzel", "Meatball",
            "Banana", "Penguin", "Llama", "Goose", "Walrus", "Burrito", "Donut", "Turnip", "Biscuit", "Badger",
            "Moose", "Gnome", "Cactus", "Toaster", "Pancake", "Sprout", "Hamster", "Yeti", "Wombat", "Jellybean",
            "Sausage", "Crumpet", "Kiwi", "Raccoon", "Dumpling", "Gizmo", "Bonker", "Honker", "Pudding", "Tater"};
    private static final String[] SPECIAL = {
            "xX_Bonk_Xx", "NoBrakes99", "HonkHonk", "GrandmaGoFast", "TotallyNotABot", "BeepBoop", "Mr. Wiggles",
            "Lord Bonkington", "Sir Honksalot", "Drift King", "ImOnPink", "Wheelie Nelson", "Bumpy McBumpface",
            "Skid Vicious", "Crashley", "Vroom Vroom", "Lagatha", "GG_EZ", "Captain Crash", "Wobble McGee"};

    public static String[] pick(Rng rng, int n) {
        String[] out = new String[n];
        HashSet<String> used = new HashSet<String>();
        for (int i = 0; i < n; i++) {
            String s;
            int guard = 0;
            do {
                if (rng.chance(0.12f)) s = SPECIAL[rng.i(SPECIAL.length)];
                else s = ADJ[rng.i(ADJ.length)] + " " + NOUN[rng.i(NOUN.length)];
                guard++;
            } while (used.contains(s) && guard < 50);
            used.add(s);
            out[i] = s;
        }
        return out;
    }
}
