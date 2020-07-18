package gelato.riso.bossapi.service.manager;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public interface ManagerRepository extends ReactiveMongoRepository<Manager, String> {
    Mono<Manager> findByUsername(String username);
}
