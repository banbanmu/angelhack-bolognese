package gelato.riso.bossapi.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import gelato.riso.bossapi.support.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> signUp(String username, String password) {
        return userRepository.findByUsername(username)
                             .flatMap(user -> {
                                 // user already exists.
                                 return Mono.error(new UserAlreadyExistException());
                             })
                             .defaultIfEmpty(User.of(username, passwordEncoder.encode(password)))
                             .flatMap(user -> userRepository.insert((User) user));
    }

    public Mono<User> signIn(String username, String password) {
        return userRepository.findByUsername(username)
                             .flatMap(user -> {
                                 String encodedPassword = user.getPassword();
                                 if (false == passwordEncoder.matches(password, encodedPassword)) {
                                     return Mono.error(new UnAuthorizedException());
                                 }

                                 return Mono.just(user);
                             }).switchIfEmpty(Mono.error(new UnAuthorizedException()));
    }

    private static final class UnAuthorizedException extends BaseException {
        private static final long serialVersionUID = 6860270788137491537L;

        @Getter
        private final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
    }

    private static final class UserAlreadyExistException extends BaseException {
        private static final long serialVersionUID = 6860270788137491537L;

        @Getter
        private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    }
}
