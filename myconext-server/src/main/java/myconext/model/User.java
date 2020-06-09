package myconext.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myconext.exceptions.WeakPasswordException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static myconext.validation.PasswordStrength.strongEnough;

@NoArgsConstructor
@Getter
@Document(collection = "users")
public class User implements Serializable, UserDetails {

    @Id
    private String id;

    private String email;
    private String givenName;
    private String familyName;
    private String uid;
    private String schacHomeOrganization;
    private String authenticatingAuthority;
    private String password;
    private boolean newUser;
    private String preferredLanguage;
    private String webAuthnIdentifier;
    private String userHandle;

    private List<LinkedAccount> linkedAccounts = new ArrayList<>();
    private Map<String, String> eduIdPerServiceProvider = new HashMap<>();
    private Map<String, String> publicKeyCredentials = new HashMap<>();
    private Map<String, List<String>> attributes = new HashMap<>();

    private long created;
    private long updatedAt = System.currentTimeMillis() / 1000L;

    public User(String uid, String email, String givenName, String familyName,
                String schacHomeOrganization, String authenticatingAuthority,
                String serviceProviderEntityId, String preferredLanguage) {
        this.uid = uid;
        this.email = email;
        this.givenName = givenName;
        this.familyName = familyName;
        this.schacHomeOrganization = schacHomeOrganization;
        this.authenticatingAuthority = authenticatingAuthority;
        this.preferredLanguage = preferredLanguage;

        this.computeEduIdForServiceProviderIfAbsent(serviceProviderEntityId);
        this.newUser = true;
        this.created = System.currentTimeMillis() / 1000L;
    }

    public void validate() {
        Assert.notNull(email, "Email is required");
        Assert.notNull(givenName, "GivenName is required");
        Assert.notNull(familyName, "FamilyName is required");
    }

    public void encryptPassword(String password, PasswordEncoder encoder) {
        if (StringUtils.hasText(password)) {
            if (!strongEnough(password)) {
                throw new WeakPasswordException();
            }
            this.password = encoder.encode(password);
        } else {
            this.password = null;
        }
    }

    @Transient
    public void addPublicKeyCredential(PublicKeyCredentialDescriptor publicKeyCredentialDescriptor,
                                       ByteArray publicKeyCredential) {
        this.publicKeyCredentials.put(
                publicKeyCredentialDescriptor.getId().getBase64Url(),
                publicKeyCredential.getBase64Url());
    }

    @Transient
    public Optional<String> computeEduIdForServiceProviderIfAbsent(String serviceProviderEntityId) {
        return StringUtils.hasText(serviceProviderEntityId) ?
                Optional.of(this.eduIdPerServiceProvider.computeIfAbsent(serviceProviderEntityId, s -> UUID.randomUUID().toString())) :
                Optional.empty();
    }


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }

    @Transient
    @JsonIgnore
    public List<LinkedAccount> linkedAccountsSorted() {
        return this.linkedAccounts.stream().sorted(Comparator.comparing(LinkedAccount::getExpiresAt).reversed()).collect(Collectors.toList());
    }

    public String getEduPersonPrincipalName() {
        return uid + "@" + schacHomeOrganization;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setWebAuthnIdentifier(String webAuthnIdentifier) {
        this.webAuthnIdentifier = webAuthnIdentifier;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public void setLinkedAccounts(List<LinkedAccount> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }
}
