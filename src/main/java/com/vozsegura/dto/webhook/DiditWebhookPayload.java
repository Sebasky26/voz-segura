package com.vozsegura.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO para procesar el payload del webhook de Didit.
 * Extrae los datos relevantes: nombre completo y número de cédula.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiditWebhookPayload {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("workflow_id")
    private String workflowId;

    @JsonProperty("verification_result")
    private VerificationResult verificationResult;

    @JsonProperty("document_data")
    private DocumentData documentData;

    @JsonProperty("vendor_data")
    private String vendorData;

    @JsonProperty("webhook_type")
    private String webhookType;

    @JsonProperty("decision")
    private Decision decision;

    // Getters y Setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public VerificationResult getVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(VerificationResult verificationResult) {
        this.verificationResult = verificationResult;
    }

    public DocumentData getDocumentData() {
        return documentData;
    }

    public void setDocumentData(DocumentData documentData) {
        this.documentData = documentData;
    }

    public String getVendorData() {
        return vendorData;
    }

    public void setVendorData(String vendorData) {
        this.vendorData = vendorData;
    }

    public String getWebhookType() {
        return webhookType;
    }

    public void setWebhookType(String webhookType) {
        this.webhookType = webhookType;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    // Inner classes

    /**
     * Resultado de la verificación
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerificationResult {

        @JsonProperty("id")
        private String id;

        @JsonProperty("status")
        private String status;

        @JsonProperty("liveness_passed")
        private Boolean livenessPassed;

        @JsonProperty("verified_at")
        private String verifiedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getLivenessPassed() {
            return livenessPassed;
        }

        public void setLivenessPassed(Boolean livenessPassed) {
            this.livenessPassed = livenessPassed;
        }

        public String getVerifiedAt() {
            return verifiedAt;
        }

        public void setVerifiedAt(String verifiedAt) {
            this.verifiedAt = verifiedAt;
        }
    }

    /**
     * Datos del documento escaneado
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentData {

        @JsonProperty("document_type")
        private String documentType;

        @JsonProperty("document_number")
        private String documentNumber;

        @JsonProperty("personal_number")
        private String personalNumber;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("date_of_birth")
        private String dateOfBirth;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("nationality")
        private String nationality;

        @JsonProperty("expiry_date")
        private String expiryDate;

        @JsonProperty("issue_date")
        private String issueDate;

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }

        public void setDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
        }

        public String getPersonalNumber() {
            return personalNumber;
        }

        public void setPersonalNumber(String personalNumber) {
            this.personalNumber = personalNumber;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(String issueDate) {
            this.issueDate = issueDate;
        }
    }

    /**
     * Estructura Decision - contiene los datos completos de la verificación
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Decision {

        @JsonProperty("status")
        private String status;

        @JsonProperty("id_verifications")
        private List<IdVerification> idVerifications;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<IdVerification> getIdVerifications() {
            return idVerifications;
        }

        public void setIdVerifications(List<IdVerification> idVerifications) {
            this.idVerifications = idVerifications;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdVerification {
            @JsonProperty("first_name")
            private String firstName;

            @JsonProperty("last_name")
            private String lastName;

            @JsonProperty("full_name")
            private String fullName;

            @JsonProperty("personal_number")
            private String personalNumber;

            @JsonProperty("status")
            private String status;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getFullName() {
                return fullName;
            }

            public void setFullName(String fullName) {
                this.fullName = fullName;
            }

            public String getPersonalNumber() {
                return personalNumber;
            }

            public void setPersonalNumber(String personalNumber) {
                this.personalNumber = personalNumber;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }
        }
    }
}
