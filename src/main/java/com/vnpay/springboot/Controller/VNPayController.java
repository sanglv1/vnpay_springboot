package com.vnpay.springboot.Controller;

import com.vnpay.springboot.Dto.PaymentRequest;
import com.vnpay.springboot.Dto.QueryRequest;
import com.vnpay.springboot.Dto.RefundRequest;
import com.vnpay.springboot.Entity.PaymentOrder;
import com.vnpay.springboot.Entity.PaymentStatus;
import com.vnpay.springboot.Repository.PaymentOrderRepository;
import com.vnpay.springboot.Service.VNPayService;
import com.vnpay.springboot.Service.VNPayQuery;
import com.vnpay.springboot.Service.VNPayRefund;
import com.vnpay.springboot.Util.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/demo")
public class VNPayController {

    private static final Logger log = LoggerFactory.getLogger(VNPayController.class);
    private static final int PAGE_SIZE = 15;

    private final VNPayService vnPayService;
    private final VNPayQuery vnPayQuery;
    private final VNPayRefund vnPayRefund;
    private final PaymentOrderRepository orderRepository;

    public VNPayController(VNPayService vnPayService, VNPayQuery vnPayQuery, VNPayRefund vnPayRefund,
                           PaymentOrderRepository orderRepository) {
        this.vnPayService = vnPayService;
        this.vnPayQuery = vnPayQuery;
        this.vnPayRefund = vnPayRefund;
        this.orderRepository = orderRepository;
    }

    // ------------------- LANDING -------------------
    @GetMapping({"", "/"})
    public String demoIndex() {
        return "demo/index";
    }

    // ------------------- THỐNG KÊ ĐƠN HÀNG -------------------
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long total = orderRepository.count();
        long pending = orderRepository.countByStatus(PaymentStatus.PENDING);
        long success = orderRepository.countByStatus(PaymentStatus.SUCCESS);
        long failed = orderRepository.countByStatus(PaymentStatus.FAILED);
        Page<PaymentOrder> recent = orderRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("total", total);
        model.addAttribute("pending", pending);
        model.addAttribute("success", success);
        model.addAttribute("failed", failed);
        model.addAttribute("recentOrders", recent.getContent());
        return "support/dashboard";
    }

    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String txnRef,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        if (page < 0) page = 0;
        if (status != null && ("null".equals(status) || status.isBlank())) status = null;
        if (txnRef != null && ("null".equals(txnRef) || txnRef.isBlank())) txnRef = null;
        if (dateFrom != null && ("null".equals(dateFrom) || dateFrom.isBlank())) dateFrom = null;
        if (dateTo != null && ("null".equals(dateTo) || dateTo.isBlank())) dateTo = null;

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentOrder> orders;
        if (status != null) {
            try {
                PaymentStatus ps = PaymentStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatusOrderByCreatedAtDesc(ps, pageable);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else if (txnRef != null) {
            orders = orderRepository.findByTxnRefContainingOrderByCreatedAtDesc(txnRef.trim(), pageable);
        } else if (dateFrom != null && dateTo != null) {
            try {
                LocalDate from = LocalDate.parse(dateFrom);
                LocalDate to = LocalDate.parse(dateTo);
                LocalDateTime start = LocalDateTime.of(from, LocalTime.MIN);
                LocalDateTime end = LocalDateTime.of(to, LocalTime.MAX);
                orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end, pageable);
            } catch (Exception e) {
                orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        model.addAttribute("orders", orders);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterTxnRef", txnRef);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        return "support/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<PaymentOrder> opt = orderRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng.");
            return "redirect:/demo/orders";
        }
        model.addAttribute("order", opt.get());
        return "support/order-detail";
    }

    // ------------------- FORM -------------------
    @GetMapping("/pay")
    public String showPayForm() {
        return "vnpay_pay";
    }

    @GetMapping("/query")
    public String querydr(
            @RequestParam(required = false) String txnRef,
            @RequestParam(required = false) String transDate,
            @RequestParam(required = false) String transactionNo,
            Model model) {
        if (txnRef != null) model.addAttribute("prefillTxnRef", txnRef);
        if (transDate != null) model.addAttribute("prefillTransDate", transDate);
        if (transactionNo != null) model.addAttribute("prefillTransactionNo", transactionNo);
        return "vnpay_querydr";
    }

    @GetMapping("/refund")
    public String refund(
            @RequestParam(required = false) String txnRef,
            @RequestParam(required = false) String transactionNo,
            @RequestParam(required = false) Long amount,
            @RequestParam(required = false) String transDate,
            Model model) {
        if (txnRef != null) model.addAttribute("prefillTxnRef", txnRef);
        if (transactionNo != null) model.addAttribute("prefillTransactionNo", transactionNo);
        if (amount != null) model.addAttribute("prefillAmount", amount);
        if (transDate != null) model.addAttribute("prefillTransDate", transDate);
        return "vnpay_refund";
    }

    // ------------------- TẠO ĐƠN HÀNG -------------------
    @PostMapping("/submitOrder")
    public String submitOrder(
            @Valid PaymentRequest paymentRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin đơn hàng.");
            return "vnpay_pay";
        }

        String clientIp = VNPayUtils.getIpAddress(request);

        try {
            String vnpayUrl = vnPayService.createOrder(
                    paymentRequest.getAmount(),
                    paymentRequest.getOrderInfo(),
                    paymentRequest.getBankcode(),
                    paymentRequest.getOrdertype(),
                    paymentRequest.getPromocode(),
                    paymentRequest.getTxnRef(),
                    clientIp
            );
            log.info("Redirecting user to VNPAY URL: {}", vnpayUrl);
            return "redirect:" + vnpayUrl;

        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "vnpay_pay";
        } catch (UnsupportedEncodingException e) {
            log.error("Error creating VNPAY URL", e);
            model.addAttribute("error", "Lỗi mã hóa URL, vui lòng thử lại.");
            return "vnpay_pay";
        }
    }

    // ------------------- RETURN / IPN -------------------
    @GetMapping("/vnpay-return")
    public String handleVnPayReturn(HttpServletRequest request, Model model) {

        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            vnpParams.put(fieldName, fieldValue);
        }

        // CHUYỂN LOGIC XỬ LÝ CHỮ KÝ VÀ TRẠNG THÁI VÀO SERVICE
        int paymentStatus = vnPayService.processVnPayReturn(vnpParams);

        // --- KHẮC PHỤC LỖI THYMELEAF SPEL ---
        // 1. Lấy chuỗi số tiền từ VNPAY (đã nhân 100)
        String vnpAmountStr = vnpParams.get("vnp_Amount");

        // 2. Chuyển đổi chuỗi số tiền này sang Long để Thymeleaf có thể chia/format
        if (vnpAmountStr != null && !vnpAmountStr.isEmpty()) {
            try {
                long vnpAmountLong = Long.parseLong(vnpAmountStr);
                // 3. Đưa giá trị kiểu Long vào Model.
                model.addAttribute("vnp_Amount", vnpAmountLong);
            } catch (NumberFormatException e) {
                log.error("Error parsing vnp_Amount: {} to Long. Using default 0.", vnpAmountStr, e);
                model.addAttribute("vnp_Amount", 0L);
            }
        } else {
            model.addAttribute("vnp_Amount", 0L);
        }
        // ------------------------------------

        // Lấy các tham số VNPAY còn lại (không cần chuyển đổi)
        model.addAttribute("vnp_TxnRef", vnpParams.get("vnp_TxnRef"));
        model.addAttribute("vnp_OrderInfo", vnpParams.get("vnp_OrderInfo"));
        model.addAttribute("vnp_BankCode", vnpParams.get("vnp_BankCode"));
        model.addAttribute("vnp_PayDate", vnpParams.get("vnp_PayDate"));
        String responseCode = vnpParams.get("vnp_ResponseCode");
        model.addAttribute("vnp_ResponseCode", responseCode);


        if (paymentStatus == -1) {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Lỗi xác thực dữ liệu (Invalid Signature).");
        } else if (paymentStatus == 1) {
            model.addAttribute("status", "success");
            model.addAttribute("message", "Thanh toán thành công!");
        } else { // paymentStatus == 0 hoặc các mã lỗi khác
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Giao dịch không thành công. Mã lỗi VNPAY: " + responseCode);
        }

        return "vnpay_return";
    }
    @GetMapping("/vnpay-ipn")
    @ResponseBody
    public Map<String, String> handleVnPayIpn(HttpServletRequest request) {

        // 1. Lấy tất cả tham số từ request
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            vnpParams.put(fieldName, fieldValue);
        }

        log.info("Received VNPAY IPN Request: {}", vnpParams);

        // 2. Chuyển logic xử lý và cập nhật DB vào Service
        Map<String, String> ipnResponse = vnPayService.processVnPayIpn(vnpParams);

        // 3. Trả về JSON Response
        return ipnResponse;
    }


    // ------------------- QUERY -------------------
    @PostMapping("/process-query")
    public String processQuery(
            @Valid QueryRequest queryRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin truy vấn.");
            return "vnpay_querydr";
        }

        String clientIp = VNPayUtils.getIpAddress(request);
        String resultJson = vnPayQuery.processQuery(
                queryRequest.getTxnRef(),
                queryRequest.getTransDate(),
                clientIp,
                queryRequest.getTransactionNo()
        );
        model.addAttribute("queryResult", resultJson);
        return "vnpay_query_result";
    }

    // ------------------- REFUND -------------------
    @PostMapping("/process-refund")
    public String processRefund(
            @Valid RefundRequest refundRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin hoàn tiền.");
            return "vnpay_refund";
        }

        String clientIp = VNPayUtils.getIpAddress(request);
        String resultJson = vnPayRefund.sendRefundRequest(
                refundRequest.getTxnRef(),
                refundRequest.getTransactionNo(),
                refundRequest.getAmount(),
                refundRequest.getTransType(),
                refundRequest.getCreateBy(),
                refundRequest.getTransDate(),
                clientIp
        );
        model.addAttribute("refundResult", resultJson);
        return "vnpay_refund_result";
    }
}
