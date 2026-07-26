package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBookingReceipt;
import com.asg.spindleserp.travel.entity.TrvPaymentModeAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrvPaymentModeAccountRepository extends JpaRepository<TrvPaymentModeAccount, Long> {

    List<TrvPaymentModeAccount> findByOrganizationId(Long organizationId);

    Optional<TrvPaymentModeAccount> findByOrganizationIdAndPaymentMode(Long organizationId, TrvBookingReceipt.PaymentMode paymentMode);
}
