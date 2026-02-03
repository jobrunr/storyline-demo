package org.jobrunr.storylinedemo.creditcards;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.datafaker.Faker;
import net.datafaker.providers.base.Finance.CreditCardType;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.Random;

public class CreditCard {

    public enum Type {
        MASTERCARD,
        AMERICAN_EXPRESS
    }

    public enum State {
        REQUESTED,
        ACTIVE,
        CANCELLED
    }

    @Id
    private Long id;

    @NotBlank(message = "CreditCard Number is required")
    @Size(min = 15, max = 16, message = "Number must be 15-16 digits")
    private String number;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotNull(message = "Credit card type is required")
    private CreditCard.Type type;

    private CreditCard.State state = State.REQUESTED;

    private BigDecimal balance = BigDecimal.ZERO;

    public CreditCard() {
        this(randomCreditCardNumber(), null, null, null);
    }

    public CreditCard(String name, String email, Type type) {
        this(randomCreditCardNumber(), name, email, type);
    }

    public CreditCard(String number, String name, String email, Type type) {
        this(number, name, email, type, State.REQUESTED);
    }

    public CreditCard(String number, String name, String email, Type type, State state) {
        this.number = number;
        this.name = name;
        this.email = email;
        this.type = type;
        this.state = state;
    }

    public static CreditCard randomCreditCard() {
        var faker = new Faker();
        var cardType = randomCreditCardType();
        var creditCardNumber = randomCreditCardNumber();
        var name = faker.name().fullName();
        var email = faker.internet().safeEmailAddress(name);
        var state = randomCreditCardState();
        return new CreditCard(creditCardNumber, name, email, cardType, state);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void activate() {
        this.state = State.ACTIVE;
    }

    public String getNumber() {
        return number;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void deductBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "number='" + number + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    private static Type randomCreditCardType() {
        return new Random().nextInt(2) == 0 ? Type.AMERICAN_EXPRESS : Type.MASTERCARD;
    }

    private static String randomCreditCardNumber() {
        return new Faker().finance().creditCard(CreditCardType.valueOf(randomCreditCardType().name())).replace("-", "");   
    }

    private static State randomCreditCardState() {
        int r = new Random().nextInt(State.values().length);
        return State.values()[r];
    }
}
