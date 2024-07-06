    package com.example.Buoi2.controller;

    import com.example.Buoi2.entity.Role;
    import com.example.Buoi2.entity.User;
    import com.example.Buoi2.repository.IRoleRepository;
    import com.example.Buoi2.repository.UserRepository;
    import com.example.Buoi2.services.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
    import org.springframework.security.oauth2.core.user.OAuth2User;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.GetMapping;

    import java.io.IOException;
    import java.util.Collections;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    import java.util.stream.Collectors;

    @Controller
    public class OAuth2LoginSuccessController {
        @Autowired
        private UserService userService;
        @Autowired
        private UserRepository userRepository;

        @Autowired
        private IRoleRepository roleRepository;

        @GetMapping("/login/oauth2/success")
        public String loginSuccess(OAuth2AuthenticationToken token) {
            OAuth2User oauth2User = token.getPrincipal();

            // Lấy thông tin từ OAuth2User
            String email = oauth2User.getAttribute("email");
            String username = oauth2User.getAttribute("name"); // hoặc các thuộc tính khác bạn muốn sử dụng làm username

            // Kiểm tra xem email có tồn tại và không rỗng
            if (email == null || email.isEmpty()) {
                // Xử lý khi email không hợp lệ, có thể trả về một trang lỗi hoặc thông báo người dùng
                return "redirect:/error-page";
            }

            // Kiểm tra xem user đã tồn tại trong cơ sở dữ liệu chưa
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                // Nếu tồn tại user, cập nhật thông tin
                User user = userOptional.get();
                user.setUsername(username); // Cập nhật username nếu cần thiết
                userRepository.save(user); // Lưu thông tin user đã cập nhật

                // Cấp lại quyền hạn cho người dùng từ cơ sở dữ liệu
                // Lấy danh sách các quyền hạn của người dùng
                Set<Role> roles = user.getRoles();

                // Cấp lại quyền hạn trong phiên làm việc (session)
                // Ví dụ sử dụng Spring Security:
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                List<GrantedAuthority> updatedAuthorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList());

                Authentication newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), updatedAuthorities);
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            } else {
                // Nếu không tồn tại user, tạo user mới và cấp role mặc định
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email);

                // Tạo và lưu role mặc định (ví dụ ROLE_USER)
                Role defaultRole = roleRepository.findByName("ROLE_USER")
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName("ROLE_USER");
                            return roleRepository.save(newRole);
                        });
                newUser.setRoles(Collections.singleton(defaultRole)); // Sử dụng Set<Role> cho roles
                userRepository.save(newUser); // Lưu user mới vào cơ sở dữ liệu
            }

            // Chuyển hướng đến trang xem đơn hàng hoặc trang chủ
            return "redirect:/orders"; // Thay đổi đường dẫn tùy vào tên đường dẫn của trang xem đơn hàng
        }

        @GetMapping("/oauth2/loginSuccess")
        private void handleOauth2LoginSuccess(OAuth2AuthenticationToken authentication) throws IOException {
            OAuth2User oauth2User = authentication.getPrincipal();

            // Lấy thông tin từ OAuth2User
            String email = oauth2User.getAttribute("email");
            String username = oauth2User.getAttribute("name");

            // Kiểm tra xem email có tồn tại và không rỗng
            if (email == null || email.isEmpty()) {
                // Xử lý khi email không hợp lệ, có thể trả về một trang lỗi hoặc thông báo người dùng
                return;
            }

            // Kiểm tra xem user đã tồn tại trong cơ sở dữ liệu chưa
            Optional<User> userOptional = userService.findByEmail(email);
            if (userOptional.isPresent()) {
                // Nếu tồn tại user, cập nhật thông tin nếu cần thiết
                User user = userOptional.get();
                user.setUsername(username); // Cập nhật username nếu cần thiết
                userService.save(user); // Lưu thông tin user đã cập nhật

                // Cấp lại quyền hạn cho người dùng từ cơ sở dữ liệu
                Set<Role> roles = user.getRoles();

                // Cấp lại quyền hạn trong phiên làm việc (session)
                // Ví dụ sử dụng Spring Security:
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                List<GrantedAuthority> updatedAuthorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList());

                Authentication newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), auth.getCredentials(), updatedAuthorities);
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            } else {
                // Nếu không tồn tại user, tạo user mới và cấp role mặc định
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email);

                // Tạo và lưu role mặc định (ví dụ ROLE_USER)
                Role defaultRole = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName("USER");
                            return roleRepository.save(newRole);
                        });
                newUser.setRoles(Collections.singleton(defaultRole)); // Sử dụng Set<Role> cho roles
                userService.save(newUser); // Lưu user mới vào cơ sở dữ liệu

                // Cấp lại quyền hạn cho người dùng mới
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                Authentication auth = new UsernamePasswordAuthenticationToken(newUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

    }
