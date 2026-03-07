package com.TalentForge.talentforge.auth.service;

import com.TalentForge.talentforge.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private record OtpEntry(String otp, Instant expiry) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.mail.otp-ttl-minutes:5}")
    private int ttlMinutes;

    public String generate(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp, Instant.now().plusSeconds((long) ttlMinutes * 60)));
        return otp;
    }

    public void validate(String email, String otp) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null || Instant.now().isAfter(entry.expiry())) {
            store.remove(email.toLowerCase());
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }
        if (!entry.otp().equals(otp)) {
            throw new BadRequestException("Invalid verification code.");
        }
        store.remove(email.toLowerCase());
    }
}
