package com.documania.backend.security;

import com.documania.backend.role.Permission;
import com.documania.backend.user.UserAccount;
import com.documania.backend.user.UserAccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + account.getRole().getName().name()));

        for (Permission permission : account.getRole().getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(permission.getCode().name()));
        }

        return User.withUsername(account.getEmail())
            .password(account.getPasswordHash())
            .authorities(authorities)
            .disabled(!account.isEnabled() || !account.isEmailVerified())
            .build();
    }
}
