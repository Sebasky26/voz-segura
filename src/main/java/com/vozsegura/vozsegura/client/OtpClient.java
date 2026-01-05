package com.vozsegura.vozsegura.client;

public interface OtpClient {

    String sendOtp(String destination);

    boolean verifyOtp(String otpId, String code);
}
