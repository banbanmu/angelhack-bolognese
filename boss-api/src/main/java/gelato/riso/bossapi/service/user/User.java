package gelato.riso.bossapi.service.user;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;

@Value
@Builder
@Document
public class User implements UserDetails {
    private static final long serialVersionUID = 3330985950829456765L;

    @Id
    ObjectId id;
    @Indexed
    String username;
    @With
    String password;
    @Default
    List<Role> roles = List.of(Role.ROLE_USER);

    public static User of(String username, String password) {
        return builder()
                .username(username)
                .password(password)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                    .map(authority -> new SimpleGrantedAuthority(authority.name()))
                    .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public String getIdToString() {
        return id.toHexString();
    }

    private enum Role {
        ROLE_USER
    }

}
