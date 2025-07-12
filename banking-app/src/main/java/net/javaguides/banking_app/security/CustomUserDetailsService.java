package net.javaguides.banking_app.security;

import net.javaguides.banking_app.entity.Account;
import net.javaguides.banking_app.repository.AccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String accountHolderName) throws UsernameNotFoundException {
        Account account = accountRepository.findByAccountHolderName(accountHolderName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(account.getAccountHolderName())   // You still set it to accountHolderName here
                .password(account.getPassword())
                .roles("USER")
                .build();
    }
}
