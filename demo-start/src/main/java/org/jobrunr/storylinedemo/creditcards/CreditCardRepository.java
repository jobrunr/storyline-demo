package org.jobrunr.storylinedemo.creditcards;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends CrudRepository<CreditCard, Long> {

    CreditCard findByEmail(String email);

    Optional<CreditCard> findByNumber(String number);

    List<CreditCard> findByState(CreditCard.State state);

    @Query("SELECT * FROM credit_card WHERE state = 'ACTIVE' ORDER BY RANDOM() LIMIT :limit")
    List<CreditCard> findRandomActiveCards(int limit);
}
