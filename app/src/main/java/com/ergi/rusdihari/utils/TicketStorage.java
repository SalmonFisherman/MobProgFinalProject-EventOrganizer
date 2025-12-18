package com.ergi.rusdihari.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TicketStorage {

    private static final String PREF = "guest_tickets_pref";
    private static final String KEY_TOKENS = "tokens_set";

    public static void saveToken(@NonNull Context context, @NonNull String token) {
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> old = sp.getStringSet(KEY_TOKENS, null);
        Set<String> copy = (old == null) ? new HashSet<>() : new HashSet<>(old);
        copy.add(token);
        sp.edit().putStringSet(KEY_TOKENS, copy).apply();
    }

    @NonNull
    public static Set<String> getTokens(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> tokens = sp.getStringSet(KEY_TOKENS, null);
        if (tokens == null) return Collections.emptySet();
        return new HashSet<>(tokens);
    }

    public static void removeToken(@NonNull Context context, @NonNull String token) {
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        Set<String> old = sp.getStringSet(KEY_TOKENS, null);
        if (old == null) return;

        Set<String> copy = new HashSet<>(old);
        copy.remove(token);
        sp.edit().putStringSet(KEY_TOKENS, copy).apply();
    }

    public static void clear(@NonNull Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TOKENS)
                .apply();
    }
}