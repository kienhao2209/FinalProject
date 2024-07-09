package com.example.Buoi2.controller;

import com.example.Buoi2.entity.Role;
import com.example.Buoi2.entity.User;
import com.example.Buoi2.services.RoleService;
import com.example.Buoi2.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final RoleService roleService;
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "users/login";
    }

    @GetMapping("/register")
    public String register(@NotNull Model model) {
        model.addAttribute("user", new User());
        return "users/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            return "users/register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("passwordMismatch", "Passwords do not match");
            return "users/register";
        }

        userService.save(user);
        userService.setDefaultRole(user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "User registered successfully!");
        return "redirect:/login";
    }
    // Mở trang danh sách người dùng
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/list";
    }
    // Mở form cập nhật thông tin
    @GetMapping("/users/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        List<Role> roles = roleService.getAllRoles();
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        return "users/edit";
    }
    //Cập nhật thông tin người dùng
    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @RequestParam("username") String username,
                           @RequestParam("email") String email,
                           @RequestParam("phone") String phone,
                           @RequestParam("address") String address,
                           @RequestParam("dateOfBirth") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                           @RequestParam("roles") List<Long> roleIds,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            user.setUsername(username);
            user.setEmail(email);
            user.setPhone(phone);
            user.setAddress(address);
            user.setDateOfBirth(dateOfBirth);

            // Cập nhật roles
            List<Role> roles = roleService.getRolesByIds(roleIds);
            user.setRoles(new HashSet<>(roles));

            userService.updateUser(user);

            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user. Please try again.");
        }
        return "redirect:/users";
    }


    // Xóa người dùng
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUserById(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        return "redirect:/users";
    }


    @GetMapping("/login/google")
    public String loginGoogle(Model model, Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String email = oauthToken.getPrincipal().getAttribute("email");
            if (email != null && !email.isEmpty()) {
                // Kiểm tra xem người dùng đã tồn tại trong cơ sở dữ liệu hay chưa
                Optional<User> existingUser = userService.findByEmail(email);

                if (existingUser.isPresent()) {
                    // Người dùng đã tồn tại trong cơ sở dữ liệu, xử lý đăng nhập
                    // Ví dụ: userService.processUserLogin(existingUser.get());
                } else {
                    // Người dùng chưa tồn tại trong cơ sở dữ liệu, tạo mới và lưu
                    User newUser = new User();
                    newUser.setEmail(email);
                    // Các thông tin khác của người dùng có thể được lấy từ Google và xử lý ở đây
                    // Ví dụ: newUser.setFirstName(oauthToken.getPrincipal().getAttribute("given_name"));

                    userService.save(newUser);

                    // Xử lý đăng nhập cho người dùng mới
                    // Ví dụ: userService.processUserLogin(newUser);
                }

                // Chuyển hướng hoặc xử lý logic tiếp theo
                return "redirect:/home";
            } else {
                // Xử lý lỗi không có email từ Google
                model.addAttribute("error", "Email not provided by Google authentication");
                return "error-page";
            }
        } else {
            // Xử lý lỗi không phải là OAuth2AuthenticationToken
            model.addAttribute("error", "Invalid authentication type");
            return "error-page";
        }
    }
    @GetMapping("/email")
    public String getUserEmail(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        if (email != null) {
            userService.saveUserEmail(email);
            return "Email saved: " + email;
        } else {
            return "Email not found";
        }
    }

}
