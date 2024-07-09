package com.example.Buoi2.controller;

import com.example.Buoi2.entity.CartItem;
import com.example.Buoi2.entity.Order;
import com.example.Buoi2.entity.User;

import com.example.Buoi2.services.OrderService;
import com.example.Buoi2.services.CartService;
import com.example.Buoi2.services.UserService;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import org.springframework.security.oauth2.core.user.OAuth2User;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;

    @GetMapping("/checkout")
    public String checkout(Model model) {
        List<CartItem> cartItems = cartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        return "/cart/checkout";
    }
    //Sau khi nhập thông tin
    @PostMapping("/submit")
    public String submitOrder(@ModelAttribute Order order, Model model, Principal principal) {
        // Kiểm tra xem principal có phải là instance của OAuth2AuthenticationToken hay không
        if (principal instanceof OAuth2AuthenticationToken) {
            // Nếu là OAuth2AuthenticationToken, lấy thông tin từ OAuth2User
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            String email = oauth2User.getAttribute("email");

            // Tạo hoặc lấy người dùng từ email
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Gắn user vào đơn hàng
            order.setUser(user);
        } else {
            // Nếu không phải là OAuth2AuthenticationToken, xử lý người dùng thông thường
            String username = principal.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

            // Gắn user vào đơn hàng
            order.setUser(user);
        }

        // Tính tổng tiền
        double totalAmount = order.getTotalAmount();
        model.addAttribute("order", order);
        model.addAttribute("totalAmount", totalAmount);

        // Lưu đơn hàng vào database và nhận lại đối tượng đã lưu
        Order savedOrder = orderService.createOrder(order.getCustomerName(), order.getShippingAddress(), order.getPhoneNumber(), order.getEmail(), order.getNotes(), cartService.getCartItems(), order.getUser().getUsername());

        // Chuyển hướng đến trang xác nhận đơn hàng với id của đơn hàng vừa lưu
        return "redirect:/order/confirmation/" + savedOrder.getId();
    }



    //Hiển thị thông tin lần cuối
    @GetMapping("/confirmation/{orderId}")
    public String showOrderConfirmation(@PathVariable Long orderId, Model model) {
        // Lấy thông tin đơn hàng từ service dựa trên orderId
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            // Nếu không tìm thấy đơn hàng, chuyển hướng về trang checkout
            return "redirect:/order/checkout";
        }

        // Thêm đối tượng đơn hàng vào model để hiển thị trên template
        model.addAttribute("order", order);
        return "cart/confirmation"; // Đảm bảo template name phù hợp với đường dẫn và tên file thực tế
    }

    // Theo dõi toàn bộ đơn hàng từ admin
    @GetMapping("/admin/orders")
    public String listAllOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "orders/list"; // Thymeleaf template name for admin
    }
    // Thay đổi tình trạng đơn hàng qua status
    @PostMapping("/admin/orders/update")
    public String updateOrderStatus(@RequestParam("id") Long id, @RequestParam("status") String status) {
        try {
            Order order = orderService.getOrderById(id);
            order.setStatus(status);
            orderService.updateOrder(order); // Cập nhật đơn hàng vào cơ sở dữ liệu
            return "redirect:/order/admin/orders";
        } catch (Exception e) {
            return "redirect:/order/admin/orders?error=true";
        }
    }

    @GetMapping("/orders")
    public String getUserOrders(Model model, Principal principal) {
        if (principal == null) {
            // Xử lý khi không có người dùng đăng nhập
            return "redirect:/login";
        }

        String username = principal.getName(); // Lấy username hoặc email của người dùng đăng nhập

        Optional<User> optionalUser;
        if (username.endsWith("@gmail.com")) {
            // Đối với người dùng đăng nhập bằng Google, lấy thông tin người dùng theo email
            optionalUser = userService.findByEmail(username);
        } else {
            // Đối với người dùng đăng nhập bằng mật khẩu, lấy thông tin người dùng theo username
            optionalUser = userService.findByUsername(username);
        }

        // Nếu người dùng tồn tại, lấy danh sách đơn hàng của người dùng
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Long userId = user.getId(); // Lấy id của người dùng
            List<Order> orders = orderService.getOrdersByUserId(userId); // Lấy danh sách đơn hàng của người dùng
            model.addAttribute("orders", orders);
        } else {
            // Xử lý khi không tìm thấy người dùng
            // Có thể chuyển hướng hoặc hiển thị thông báo lỗi
        }

        return "orders/user-orders"; // Trả về view để hiển thị danh sách đơn hàng
    }

    // Hiển thị chi tiết đơn hàng dựa vào orderId
    @GetMapping("/orders/{orderId}")
    public String getOrderDetails(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrderById(orderId); // Tìm đơn hàng theo orderId
        model.addAttribute("order", order); // Đưa đối tượng order vào model để hiển thị
        return "orders/order-details"; // Trả về view để hiển thị chi tiết đơn hàng
    }
}

