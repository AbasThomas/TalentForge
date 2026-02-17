package com.TalentForge.talentforge.user.entity;

import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.note.entity.Note;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_role")
    private UserRole secondaryRole;

    private String company;

    private String phone;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> jobs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Note> notes = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Subscription subscription;

    public List<UserRole> getAvailableRoles() {
        List<UserRole> roles = new ArrayList<>();
        if (role != null) {
            roles.add(role);
        }
        if (secondaryRole != null && secondaryRole != role) {
            roles.add(secondaryRole);
        }
        return roles;
    }

    public boolean hasRole(UserRole targetRole) {
        if (targetRole == null) {
            return false;
        }
        return role == targetRole || secondaryRole == targetRole;
    }

    public void addRole(UserRole targetRole) {
        if (targetRole == null || hasRole(targetRole)) {
            return;
        }
        if (secondaryRole == null) {
            secondaryRole = targetRole;
            return;
        }
        throw new IllegalStateException("User already has two roles");
    }

    public void switchToRole(UserRole targetRole) {
        if (targetRole == null || role == targetRole) {
            return;
        }
        if (secondaryRole == targetRole) {
            secondaryRole = role;
            role = targetRole;
            return;
        }
        throw new IllegalStateException("User does not have target role");
    }
}
