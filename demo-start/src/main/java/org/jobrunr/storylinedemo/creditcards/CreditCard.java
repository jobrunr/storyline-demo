package org.jobrunr.storylinedemo.creditcards;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;

public class CreditCard {

    @Id
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotNull(message = "Credit card type is required")
    private CardType type;

    public CreditCard() {
    }

    public CreditCard(String name, String email, CardType type) {
        this.name = name;
        this.email = email;
        this.type = type;
    }

    public static CreditCard randomCreditCard(int index) {
        var cardType = index % 2 == 0 ? CardType.AMERICAN_EXPRESS : CardType.MASTERCARD;
        return new CreditCard("Random Name #" + index, "random.email" + index + "@gmail.com", cardType);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType cardType) {
        this.type = cardType;
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

    @Override
    public String toString() {
        return "CreditCard{" +
                "id='" + id + '\'' +
                "type='" + type + '\'' +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
