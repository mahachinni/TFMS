package com.tfms.security;

import com.tfms.model.User;
import com.tfms.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return new CustomUserDetails(user);
    }
    
    /**
     * Custom UserDetails implementation that wraps our User entity
     */
    public static class CustomUserDetails implements UserDetails {
        
        private final User user;
        
        public CustomUserDetails(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getUsername();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return user.isAccountNonExpired();
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return user.isAccountNonLocked();
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return user.isCredentialsNonExpired();
        }
        
        @Override
        public boolean isEnabled() {
            return user.isEnabled();
        }
        
        public User getUser() {
            return user;
        }
        
        public String getFullName() {
            return user.getFullName();
        }
        
        public String getRole() {
            return user.getRole().name();
        }
    }
}
