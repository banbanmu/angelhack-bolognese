package gelato.riso.bossapi.service.store;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
    Category category;
    List<Food> menu;

    @Value
    @Document
    public static class Food {
        String name;
        Integer price;
        String description;
        String lastCookVideoUrl;
    }

    @RequiredArgsConstructor
    public enum Category {
        KOREAN("한식"),
        JAPANESE("일식"),
        ASIAN("아시안"),
        CHINESE("중식"),
        WESTERN("양식");

        @Getter
        private final String korean;
    }

}
