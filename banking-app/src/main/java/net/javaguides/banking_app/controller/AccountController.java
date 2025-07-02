package net.javaguides.banking_app.controller;

import net.javaguides.banking_app.dto.AccountDto;
import net.javaguides.banking_app.entity.Account;
import net.javaguides.banking_app.entity.AccountDetails;
import net.javaguides.banking_app.entity.Transaction;
import net.javaguides.banking_app.mapper.AccountMapper;
import net.javaguides.banking_app.repository.AccountDetailsRepository;
import net.javaguides.banking_app.repository.AccountRepository;
import net.javaguides.banking_app.repository.TransactionRepository;
import net.javaguides.banking_app.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private AccountService accountService;
    private AccountRepository accountRepository;
    private final TransactionRepository transactionRepo;
    private final AccountDetailsRepository accountDetailsRepo;


    public AccountController(AccountService accountService, AccountRepository accountRepository, TransactionRepository transactionRepo, AccountDetailsRepository accountDetailsRepo) {

        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepo = transactionRepo;
        this.accountDetailsRepo = accountDetailsRepo;
    }

    //Add account REST API
    @PostMapping
    public ResponseEntity<?> addAccount(@RequestBody Map<String, String> requestData) {

        String password = requestData.get("password");

        // Password validation: min 1 uppercase, 1 lowercase, 1 symbol, min 8 chars
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=\\-{};:'\",.<>?/]).{8,}$")) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters, include uppercase, lowercase, and a special symbol.");
        }

        // Save to Accounts table
        AccountDto accountDto = new AccountDto();
        accountDto.setAccountHolderName(requestData.get("accountHolderName"));
        accountDto.setBalance(Double.parseDouble(requestData.get("balance")));
        accountDto.setPassword(password);

        AccountDto savedAccount = accountService.createAccount(accountDto);

        // Save to AccountDetails table
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


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> loginData) {
        String accountHolderName = loginData.get("accountHolderName");
        String password = loginData.get("password");

        Optional<Account> accountOptional = accountRepository.findAll()
                .stream()
                .filter(acc -> acc.getAccountHolderName().equals(accountHolderName))
                .findFirst();

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            if (account.getPassword().equals(password)) {
                return ResponseEntity.ok("Login Successful...");
            } else {
                return ResponseEntity.status(401).body("Invalid credentials...");
            }
        } else {
            return ResponseEntity.status(401).body("Invalid credentials...");
        }
    }



    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody Map<String, Object> requestData) {
        String name = (String) requestData.get("accountHolderName");
        String password = (String) requestData.get("password");
        double amount = Double.parseDouble(requestData.get("amount").toString());

        Optional<Account> accountOptional = accountRepository.findAll().stream()
                .filter(acc -> acc.getAccountHolderName().equals(name) && acc.getPassword().equals(password))
                .findFirst();

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            account.setBalance(account.getBalance() + amount);
            accountRepository.save(account);
            return ResponseEntity.ok("Deposit successful");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }


    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAmount(@RequestBody Map<String, Object> requestData) {
        String accountHolderName = (String) requestData.get("accountHolderName");
        String password = (String) requestData.get("password");
        double amount = Double.parseDouble(requestData.get("amount").toString());

        // Find account by name
        Optional<Account> optionalAccount = accountRepository.findAll().stream()
                .filter(acc -> acc.getAccountHolderName().equals(accountHolderName))
                .findFirst();

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = optionalAccount.get();

        // Verify password
        if (!account.getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect password");
        }

        // Check balance
        if (account.getBalance() < amount) {
            return ResponseEntity.badRequest().body("Insufficient Balance");
        }

        // Perform withdraw
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        return ResponseEntity.ok("Withdrawal successful");
    }


    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@RequestBody Map<String, String> requestData) {
        String name = requestData.get("accountHolderName");
        String password = requestData.get("password");

        Optional<Account> accountOptional = accountRepository.findAll().stream()
                .filter(acc -> acc.getAccountHolderName().equals(name) && acc.getPassword().equals(password))
                .findFirst();

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            accountRepository.delete(account);
            return ResponseEntity.ok("Account deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    //Transfer or withdraw money from the account of another person
    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestBody Map<String, Object> requestData) {

        String senderName = (String) requestData.get("accountHolderName");
        String senderPassword = (String) requestData.get("password");
        String recipientAccountNumber = (String) requestData.get("recipientAccountNumber");
        double amount = Double.parseDouble(requestData.get("amount").toString());

        Optional<Account> senderOptional = accountRepository.findAll().stream()
                .filter(acc -> acc.getAccountHolderName().equals(senderName) && acc.getPassword().equals(senderPassword))
                .findFirst();

        if (senderOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid sender credentials");
        }

        Account sender = senderOptional.get();

        Optional<Account> recipientOptional = accountRepository.findAll().stream()
                .filter(acc -> acc.getAccountNumber().equals(recipientAccountNumber))
                .findFirst();

        if (recipientOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient account not found");
        }

        Account recipient = recipientOptional.get();

        if (sender.getBalance() < amount) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        // Update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(recipient);

        // Save transaction records
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

    @PostMapping("/view-account")
    public ResponseEntity<Map<String, Object>> viewAccount(@RequestBody Map<String, String> requestData) {

        String accountHolderName = requestData.get("accountHolderName");
        String password = requestData.get("password");

        // Find by account_holder_name
        Optional<AccountDetails> detailsOptional = accountDetailsRepo.findAll().stream()
                .filter(details -> details.getAccountHolderName().equals(accountHolderName)
                        && details.getPassword().equals(password))
                .findFirst();

        if (detailsOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
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