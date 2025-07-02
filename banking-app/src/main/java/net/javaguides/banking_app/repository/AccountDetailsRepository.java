package net.javaguides.banking_app.repository;

import net.javaguides.banking_app.entity.AccountDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountDetailsRepository extends JpaRepository<AccountDetails, String> {
    Optional<AccountDetails> findByAccountHolderNameAndPassword(String accountHolderName, String password);
}
