package com.smartstadium.auth;

import com.smartstadium.domain.user.Account;
import com.smartstadium.domain.user.User;

/**
 * Bridges login Accounts and the domain User that Booking needs.
 * Used by the booking module: new Booking(AccountUserMapper.toUser(account), ...).
 */
public final class AccountUserMapper {
    private AccountUserMapper() {}

    public static User toUser(Account account) {
        return new User(account.getId(), account.getFullName(),
                account.getEmail(), account.getRole());
    }
}
