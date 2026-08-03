package com.jiaozhilong.gisagent.security;

import com.jiaozhilong.gisagent.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public PlatformUserDetailsService(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .map(PlatformUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }
}
