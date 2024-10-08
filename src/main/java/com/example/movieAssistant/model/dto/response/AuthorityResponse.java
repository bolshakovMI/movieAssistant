package com.example.movieAssistant.model.dto.response;

import com.example.movieAssistant.model.db.entity.Authority;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthorityResponse {
    String username;
    List<Authority> authorities = new ArrayList<Authority>();
}
