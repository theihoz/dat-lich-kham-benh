package com.datlichkhambenh.backend.security;

import com.datlichkhambenh.backend.entity.User;
import com.datlichkhambenh.backend.repository.UserRepository;
import com.datlichkhambenh.backend.repository.DoctorRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomUserDetailsService -- Load UserDetails tu DB theo email.
 * Duoc Spring Security dung khi xac thuc.
 *
 * Authority duoc set truc tiep theo role (ADMIN / DOCTOR / PATIENT),
 * khong them tien to ROLE_ de khop voi hasAuthority() tren Controller.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;

    public CustomUserDetailsService(UserRepository userRepository, DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Khong tim thay tai khoan voi email: " + email));

        // Boolean wrapper: Lombok @Getter tao getIsActive() (khong phai isIsActive())
        boolean isActive = Boolean.TRUE.equals(user.getIsActive());
        boolean isApprovedDoctor = !"DOCTOR".equals(user.getRole())
                || doctorRepository.findById(user.getId())
                .map(doctor -> Boolean.TRUE.equals(doctor.getIsApproved()))
                .orElse(false);
        boolean verified = !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")) || user.isEmailVerified();
        boolean canAccess = isActive && isApprovedDoctor && verified;

        // Dung truc tiep user.getRole() (vi du: "ADMIN") - khong them "ROLE_"
        // de khop voi @PreAuthorize("hasAuthority('ADMIN')") tren Controller
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .accountLocked(!canAccess)
                .disabled(!isActive)
                .build();
    }
}
