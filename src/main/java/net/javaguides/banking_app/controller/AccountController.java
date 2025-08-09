package net.javaguides.banking_app.controller;

import net.javaguides.banking_app.dto.AccountDto;
import net.javaguides.banking_app.entity.Account;
import net.javaguides.banking_app.entity.AccountDetails;
import net.javaguides.banking_app.entity.Transaction;
import net.javaguides.banking_app.mapper.AccountMapper;
import net.javaguides.banking_app.repository.AccountDetailsRepository;
import net.javaguides.banking_app.repository.AccountRepository;
import net.javaguides.banking_app.repository.TransactionRepository;
import net.javaguides.banking_app.security.JwtUtil;
import net.javaguides.banking_app.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins =
        {"http://localhost:5173", "https://6896e4ec3f756d5eb97140c5--banking-portal.netlify.app/"})
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private AccountService accountService;
    private AccountRepository accountRepository;
    private final TransactionRepository transactionRepo;
    private final AccountDetailsRepository accountDetailsRepo;
    private final JwtUtil jwtUtil;

    public AccountController(AccountService accountService, AccountRepository accountRepository, TransactionRepository transactionRepo, AccountDetailsRepository accountDetailsRepo, JwtUtil jwtUtil) {

        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepo = transactionRepo;
        this.accountDetailsRepo = accountDetailsRepo;
        this.jwtUtil = jwtUtil;
    }

    //Add account REST API
    @PostMapping
    public ResponseEntity<?> addAccount(@RequestBody Map<String, String> requestData) {

        String password = requestData.get("password");
        String balanceStr = requestData.get("balance");

        if (password == null || balanceStr == null) {
            return ResponseEntity.badRequest().body("Missing fields");
        }

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=\\-{};:'\",.<>?/]).{8,}$")) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters, include uppercase, lowercase, and a special symbol.");
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid balance format");
        }

        AccountDto accountDto = new AccountDto();
        accountDto.setAccountHolderName(requestData.get("accountHolderName"));
        accountDto.setBalance(balance);
        accountDto.setPassword(password);

        AccountDto savedAccount = accountService.createAccount(accountDto);

        AccountDetails details = new AccountDetails();
        details.setAccountNumber(savedAccount.getAccountNumber());
        details.setAccountHolderName(savedAccount.getAccountHolderName());
        details.setPassword(savedAccount.getPassword());
        details.setEmail(requestData.get("email"));
        details.setAddress(requestData.get("address"));
        details.setState(requestData.get("state"));

        accountDetailsRepo.save(details);

        return new ResponseEntity<>(savedAccount, HttpStatus.CREATED);
    }



    @GetMapping("/my-account")
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7); // Remove "Bearer "
        String accountHolderName = jwtUtil.extractUsername(token);

        Optional<Account> optionalAccount = accountRepository.findByAccountHolderName(accountHolderName);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = optionalAccount.get();

        Map<String, Object> response = new HashMap<>();
        response.put("accountNumber", account.getAccountNumber());
        response.put("accountHolderName", account.getAccountHolderName());
        response.put("balance", account.getBalance());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestHeader("Authorization") String authHeader,
                                          @RequestBody Map<String, Object> requestData) {
        String token = authHeader.substring(7);
        String accountHolderName = jwtUtil.extractUsername(token);
        double amount = Double.parseDouble(requestData.get("amount").toString());

        Optional<Account> optionalAccount = accountRepository.findByAccountHolderName(accountHolderName);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = optionalAccount.get();
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        return ResponseEntity.ok("Deposit successful");
    }


    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAmount(@RequestBody Map<String, Object> requestData) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String accountHolderName = auth.getName();  // Extracted from validated token

        double amount = Double.parseDouble(requestData.get("amount").toString());

        Optional<Account> optionalAccount = accountRepository.findByAccountHolderName(accountHolderName);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = optionalAccount.get();

        if (account.getBalance() < amount) {
            return ResponseEntity.badRequest().body("Insufficient Balance");
        }

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        return ResponseEntity.ok("Withdrawal successful");
    }



    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        String accountHolderName = jwtUtil.extractUsername(token);

        Optional<Account> accountOptional = accountRepository.findByAccountHolderName(accountHolderName);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            accountRepository.delete(account);

            // Also delete account details if available
            Optional<AccountDetails> detailsOptional = accountDetailsRepo.findByAccountNumber(account.getAccountNumber());
            detailsOptional.ifPresent(accountDetailsRepo::delete);

            return ResponseEntity.ok("Account deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }
    }



    //Transfer or withdraw money from the account of another person
    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestHeader("Authorization") String authHeader,
                                                @RequestBody Map<String, Object> requestData) {

        String token = authHeader.substring(7);
        String senderName = jwtUtil.extractUsername(token); // extract from JWT

        String recipientAccountNumber = (String) requestData.get("recipientAccountNumber");
        double amount = Double.parseDouble(requestData.get("amount").toString());

        Optional<Account> senderOptional = accountRepository.findByAccountHolderName(senderName);
        if (senderOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid sender");
        }

        Account sender = senderOptional.get();

        Optional<Account> recipientOptional = accountRepository.findByAccountNumber(recipientAccountNumber);
        if (recipientOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient account not found");
        }

        Account recipient = recipientOptional.get();

        if (sender.getBalance() < amount) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        // Perform transfer
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(recipient);

        // Log transactions
        Transaction senderTxn = new Transaction();
        senderTxn.setAccountNumber(sender.getAccountNumber());
        senderTxn.setType("Transfer Sent");
        senderTxn.setAmount(-amount);
        senderTxn.setTimestamp(java.time.LocalDateTime.now());
        transactionRepo.save(senderTxn);

        Transaction recipientTxn = new Transaction();
        recipientTxn.setAccountNumber(recipient.getAccountNumber());
        recipientTxn.setType("Transfer Received");
        recipientTxn.setAmount(amount);
        recipientTxn.setTimestamp(java.time.LocalDateTime.now());
        transactionRepo.save(recipientTxn);

        return ResponseEntity.ok("Transfer successful");
    }


    @GetMapping("/view-account")
    public ResponseEntity<?> viewAccount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // remove "Bearer "
        String accountHolderName = jwtUtil.extractUsername(token);

        Optional<AccountDetails> detailsOptional = accountDetailsRepo.findAll().stream()
                .filter(details -> details.getAccountHolderName().equals(accountHolderName))
                .findFirst();

        if (detailsOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account details not found");
        }

        AccountDetails details = detailsOptional.get();

        Map<String, Object> response = new HashMap<>();
        response.put("accountNumber", details.getAccountNumber());
        response.put("accountHolderName", details.getAccountHolderName());
        response.put("email", details.getEmail());
        response.put("address", details.getAddress());
        response.put("state", details.getState());

        return ResponseEntity.ok(response);
    }

}