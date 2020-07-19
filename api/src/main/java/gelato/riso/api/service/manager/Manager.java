package gelato.riso.api.service.manager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.With;

@Value
@Builder
@Document
public class Manager implements UserDetails {
    private static final long serialVersionUID = 3330985950829456765L;

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    @Id
    @Default
    Integer id = COUNTER.getAndIncrement();
    @Indexed
    String username;
    @With
    String password;
    String phoneNumber;
    @Default
    List<Role> roles = Lists.newArrayList(Role.ROLE_USER);

    public static Manager of(String username, String password, String phoneNumber) {
        return builder()
                .username(username)
                .password(password)
                .phoneNumber(phoneNumber)
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

    private enum Role {
        ROLE_USER
    }

}
