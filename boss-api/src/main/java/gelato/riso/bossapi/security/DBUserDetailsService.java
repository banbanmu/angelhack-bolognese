package gelato.riso.bossapi.security;

import java.util.function.Function;

import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import gelato.riso.bossapi.service.manager.Manager;
import gelato.riso.bossapi.service.manager.ManagerRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DBUserDetailsService implements ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    private final ManagerRepository managerRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return managerRepository.findByUsername(username)
                                .map(Function.identity());
    }

    @Override
    public Mono<UserDetails> updatePassword(UserDetails userDetails, String newPassword) {
        Manager manager = (Manager) userDetails;
        return managerRepository.save(manager.withPassword(newPassword))
                                .map(Function.identity());
    }

}
