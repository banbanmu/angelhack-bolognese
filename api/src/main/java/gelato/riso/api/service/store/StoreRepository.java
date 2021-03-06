package gelato.riso.api.service.store;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends ReactiveMongoRepository<Store, Integer> {
}
