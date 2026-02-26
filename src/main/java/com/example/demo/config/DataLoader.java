package com.example.demo.config;

import com.example.demo.entity.Item;
import com.example.demo.repository.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataLoader {

    @Bean
    @Profile("!test")
    CommandLineRunner loadSampleData(ItemRepository itemRepository) {
        return args -> {
            if (itemRepository.count() == 0) {
                for (int i = 1; i <= 50; i++) {
                    itemRepository.save(new Item("Item " + i, "Description for item " + i));
                }
            }
        };
    }
}
