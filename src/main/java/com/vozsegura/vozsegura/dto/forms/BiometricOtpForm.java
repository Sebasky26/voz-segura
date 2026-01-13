package com.vozsegura.vozsegura.dto.forms;

import org.springframework.web.multipart.MultipartFile;

public class BiometricOtpForm {

    private MultipartFile biometricSample;

    public MultipartFile getBiometricSample() { return biometricSample; }
    public void setBiometricSample(MultipartFile biometricSample) { this.biometricSample = biometricSample; }
}
