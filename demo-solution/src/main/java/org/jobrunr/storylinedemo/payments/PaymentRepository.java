package org.jobrunr.storylinedemo.payments;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Long> {

    List<Payment> findByCreditCardId(Long creditCardId);

    List<Payment> findByStatus(Payment.Status status);
}
