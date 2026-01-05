package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ComplaintForm {

    @NotBlank
    @Size(max = 4000)
    private String detail;

    private MultipartFile[] evidences;

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Size(max = 512)
    private String companyAddress;

    @NotBlank
    @Size(max = 255)
    private String companyContact;

    @Email
    @Size(max = 255)
    private String companyEmail;

    @Size(max = 20)
    private String companyPhone;

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public MultipartFile[] getEvidences() { return evidences; }
    public void setEvidences(MultipartFile[] evidences) { this.evidences = evidences; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    public String getCompanyContact() { return companyContact; }
    public void setCompanyContact(String companyContact) { this.companyContact = companyContact; }

    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }

    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
}
