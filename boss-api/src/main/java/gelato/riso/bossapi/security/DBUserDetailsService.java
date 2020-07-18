package gelato.riso.bossapi.security;

import java.util.function.Function;

import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import gelato.riso.bossapi.service.user.UserRepository;
import gelato.riso.bossapi.service.user.User;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DBUserDetailsService implements ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                             .map(Function.identity());
    }

    @Override
    public Mono<UserDetails> updatePassword(UserDetails userDetails, String newPassword) {
        User user = (User) userDetails;
        return userRepository.save(user.withPassword(newPassword))
                             .map(Function.identity());
    }

}
