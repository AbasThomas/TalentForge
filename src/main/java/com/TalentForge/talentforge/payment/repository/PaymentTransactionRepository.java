package com.TalentForge.talentforge.payment.repository;

import com.TalentForge.talentforge.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByReference(String reference);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
