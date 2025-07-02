package net.javaguides.banking_app.mapper;

import net.javaguides.banking_app.dto.AccountDto;
import net.javaguides.banking_app.entity.Account;

public class AccountMapper {
    public static Account mapToAccount(AccountDto accountDto){
        Account account = new Account(
                accountDto.getAccountNumber(),
                accountDto.getAccountHolderName(),
                accountDto.getBalance(),
                accountDto.getPassword()
        );

        return account;
    }

    public static AccountDto mapToAccountDto(Account account){

        AccountDto accountDto = new AccountDto(
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getBalance(),
                account.getPassword()

        );

        return accountDto;
    }
}
