package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DenunciaAccessForm {

    @NotBlank
    @Size(max = 20)
    private String cedula;

    @NotBlank
    @Size(max = 20)
    private String codigoDactilar;

    @NotBlank
    @Size(max = 16)
    private String captcha;

    private boolean termsAccepted;

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getCodigoDactilar() { return codigoDactilar; }
    public void setCodigoDactilar(String codigoDactilar) { this.codigoDactilar = codigoDactilar; }

    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }
}
