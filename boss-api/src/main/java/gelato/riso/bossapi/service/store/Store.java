package gelato.riso.bossapi.service.store;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Document
public class Store {
    @Id
    ObjectId id;
    String name;
    String address;
    String phoneNumber;
    String category;
    List<Food> menu;

    @Value
    @Document
    public static class Food {
        String name;
        Integer price;
        String description;
        String lastCookVideoUrl;
    }

}
