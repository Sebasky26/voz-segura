package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class BiometricOtpForm {

    private MultipartFile biometricSample;

    @NotBlank
    private String otpCode;

    public MultipartFile getBiometricSample() { return biometricSample; }
    public void setBiometricSample(MultipartFile biometricSample) { this.biometricSample = biometricSample; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
