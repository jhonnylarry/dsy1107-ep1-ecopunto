package cl.duoc.jolarraguibel.ecopunto.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Convierte claims del JWT en authorities de Spring Security.
 *
 * scp = "reportes.read reportes.write" -> SCOPE_reportes.read, SCOPE_reportes.write
 * roles = ["ENCARGADO"]                -> ROLE_ENCARGADO
 */
public class AuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Collection<GrantedAuthority> scopes = scopesConverter.convert(jwt);
        if (scopes != null) {
            authorities.addAll(scopes);
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);
        }

        return authorities;
    }
}
