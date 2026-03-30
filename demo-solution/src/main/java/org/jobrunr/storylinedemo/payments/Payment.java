package org.jobrunr.storylinedemo.payments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import net.datafaker.Faker;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Payment {

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    private Long id;

    @NotNull(message = "Credit card is required")
    private Long creditCardId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Platform is required")
    private PaymentPlatform platform;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    private Status status = Status.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {
    }

    public Payment(Long creditCardId, BigDecimal amount, PaymentPlatform platform, String recipient) {
        this.creditCardId = creditCardId;
        this.amount = amount;
        this.platform = platform;
        this.recipient = recipient;
    }

    public static Payment randomPayment(Long creditCardId, List<String> activeCardNumbers) {
        var faker = new Faker();
        var platform = PaymentPlatform.values()[faker.random().nextInt(PaymentPlatform.values().length)];
        var amount = BigDecimal.valueOf(faker.random().nextDouble(1, 15000));
        String recipient = switch (platform) {
            case PAYPAL -> faker.internet().emailAddress();
            case STRIPE -> "acct_" + faker.expression("#{letterify '????????????????'}");
            case JOBRUNR_FINANCE -> activeCardNumbers.get(faker.random().nextInt(activeCardNumbers.size()));
        };
        return new Payment(creditCardId, amount, platform, recipient);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreditCardId() {
        return creditCardId;
    }

    public void setCreditCardId(Long creditCardId) {
        this.creditCardId = creditCardId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PaymentPlatform platform) {
        this.platform = platform;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean requiresGovernmentReporting() {
        return amount.compareTo(BigDecimal.valueOf(10000)) > 0;
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", amount=" + amount + ", platform=" + platform + ", recipient='" + recipient + "'}";
    }
}
