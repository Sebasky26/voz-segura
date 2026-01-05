package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.OtpClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "default"})
public class MockOtpClient implements OtpClient {

    @Override
    public String sendOtp(String destination) {
        return "MOCK-OTP-ID";
    }

    @Override
    public boolean verifyOtp(String otpId, String code) {
        return "123456".equals(code);
    }
}
