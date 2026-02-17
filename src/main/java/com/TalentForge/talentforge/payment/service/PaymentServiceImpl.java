package com.TalentForge.talentforge.payment.service;

import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeRequest;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeResponse;
import com.TalentForge.talentforge.payment.dto.PaymentHistoryItemResponse;
import com.TalentForge.talentforge.payment.dto.PaymentOptionsResponse;
import com.TalentForge.talentforge.payment.dto.PaymentVerifyResponse;
import com.TalentForge.talentforge.payment.entity.BillingCycle;
import com.TalentForge.talentforge.payment.entity.PaymentCurrency;
import com.TalentForge.talentforge.payment.entity.PaymentStatus;
import com.TalentForge.talentforge.payment.entity.PaymentTransaction;
import com.TalentForge.talentforge.payment.repository.PaymentTransactionRepository;
import com.TalentForge.talentforge.notification.entity.NotificationType;
import com.TalentForge.talentforge.notification.service.NotificationService;
import com.TalentForge.talentforge.subscription.entity.PlanType;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import com.TalentForge.talentforge.subscription.repository.SubscriptionRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of(
            "card",
            "bank",
            "ussd",
            "qr",
            "mobile_money",
            "bank_transfer",
            "eft"
    );

    private static final Map<PlanType, Map<BillingCycle, Long>> RECRUITER_PLAN_USD_PRICE_MINOR = Map.of(
            PlanType.BASIC, Map.of(BillingCycle.MONTHLY, 1900L, BillingCycle.ANNUAL, 18000L),
            PlanType.PRO, Map.of(BillingCycle.MONTHLY, 4900L, BillingCycle.ANNUAL, 48000L),
            PlanType.ENTERPRISE, Map.of(BillingCycle.MONTHLY, 19900L)
    );

    private static final Map<PlanType, Map<BillingCycle, Long>> CANDIDATE_PLAN_USD_PRICE_MINOR = Map.of(
            PlanType.BASIC, Map.of(BillingCycle.MONTHLY, 600L, BillingCycle.ANNUAL, 5800L),
            PlanType.PRO, Map.of(BillingCycle.MONTHLY, 1200L, BillingCycle.ANNUAL, 11500L),
            PlanType.ENTERPRISE, Map.of(BillingCycle.MONTHLY, 3900L, BillingCycle.ANNUAL, 37400L)
    );

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${app.paystack.secret-key:}")
    private String paystackSecretKey;

    @Value("${app.paystack.webhook-secret:}")
    private String paystackWebhookSecret;

    @Value("${app.paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    @Value("${app.paystack.callback-url:}")
    private String paystackCallbackUrl;

    @Value("${app.frontend.public-base-url:http://localhost:3000}")
    private String frontendPublicBaseUrl;

    @Value("${app.paystack.channels:card,bank,ussd,qr,mobile_money,bank_transfer,eft}")
    private String paystackChannels;

    @Value("${app.paystack.supported-currencies:USD,NGN,GHS,KES,ZAR}")
    private String paystackSupportedCurrencies;

    @Value("${app.paystack.default-currency:USD}")
    private String paystackDefaultCurrency;

    @Value("${app.paystack.auto-detect-currency:true}")
    private boolean paystackAutoDetectCurrency;

    @Value("${app.paystack.fx.ngn:1500}")
    private BigDecimal ngnRate;

    @Value("${app.paystack.fx.ghs:15.5}")
    private BigDecimal ghsRate;

    @Value("${app.paystack.fx.kes:130}")
    private BigDecimal kesRate;

    @Value("${app.paystack.fx.zar:18.5}")
    private BigDecimal zarRate;

    @Override
    @Transactional(readOnly = true)
    public PaymentOptionsResponse getOptions() {
        User currentUser = getAuthenticatedUserOptional();
        UserRole pricingRole = resolvePricingRole(currentUser);
        List<String> channels = resolveChannels(null);
        List<PaymentCurrency> currencies = resolveSupportedCurrencies();
        List<PaymentOptionsResponse.PaymentPriceOption> prices = new ArrayList<>();

        for (PlanType planType : List.of(PlanType.BASIC, PlanType.PRO, PlanType.ENTERPRISE)) {
            PlanLimits limits = resolvePlanLimits(planType, pricingRole);
            for (BillingCycle cycle : BillingCycle.values()) {
                for (PaymentCurrency currency : currencies) {
                    Long amountUsdMinor = resolveUsdMinorAmount(planType, cycle, pricingRole);
                    if (amountUsdMinor == null) {
                        prices.add(new PaymentOptionsResponse.PaymentPriceOption(
                                planType,
                                cycle,
                                currency,
                                0L,
                                0L,
                                limits.jobPostLimit(),
                                limits.applicantLimit(),
                                limits.applicationLimit(),
                                limits.resumeScoreLimit(),
                                false,
                                "Custom quote required"
                        ));
                        continue;
                    }

                    prices.add(new PaymentOptionsResponse.PaymentPriceOption(
                            planType,
                            cycle,
                            currency,
                            convertUsdMinor(amountUsdMinor, currency),
                            amountUsdMinor,
                            limits.jobPostLimit(),
                            limits.applicantLimit(),
                            limits.applicationLimit(),
                            limits.resumeScoreLimit(),
                            true,
                            "Paystack checkout"
                    ));
                }
            }
        }

        return new PaymentOptionsResponse(currencies, channels, prices);
    }

    @Override
    public PaymentInitializeResponse initialize(PaymentInitializeRequest request) {
        assertPaystackConfigured();
        User currentUser = getAuthenticatedUser();
        assertPaymentAllowedRole(currentUser);
        validateCurrencySupported(request.currency());

        if (request.planType() == PlanType.FREE) {
            throw new BadRequestException("Free plan does not require payment");
        }

        UserRole pricingRole = resolvePricingRole(currentUser);
        Long amountUsdMinor = resolveUsdMinorAmount(request.planType(), request.billingCycle(), pricingRole);
        if (amountUsdMinor == null) {
            throw new BadRequestException("Selected plan and billing cycle requires a custom quote");
        }

        PaymentCurrency requestedCurrency = request.currency() == null ? resolveDefaultCurrency() : request.currency();
        if (!paystackAutoDetectCurrency) {
            validateCurrencySupported(requestedCurrency);
        }

        PaystackInitializeResult initializeResult = initializeTransactionWithAutoCurrency(
                currentUser,
                request,
                amountUsdMinor,
                requestedCurrency
        );

        List<String> channels = resolveChannels(request.channels());
        JsonNode data = initializeResult.root().path("data");

        String authorizationUrl = readText(data, "authorization_url");
        String accessCode = readText(data, "access_code");
        String paystackReference = readText(data, "reference");

        if (isBlank(paystackReference)) {
            throw new BadRequestException("Paystack did not return a payment reference");
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(currentUser)
                .planType(request.planType())
                .billingCycle(request.billingCycle())
                .currency(initializeResult.currency())
                .amountMinor(initializeResult.amountMinor())
                .amountUsdMinor(amountUsdMinor)
                .reference(paystackReference)
                .accessCode(accessCode)
                .authorizationUrl(authorizationUrl)
                .status(PaymentStatus.PENDING)
                .gatewayStatus("pending")
                .build();
        paymentTransactionRepository.save(transaction);

        return new PaymentInitializeResponse(
                transaction.getReference(),
                transaction.getAuthorizationUrl(),
                transaction.getAccessCode(),
                transaction.getPlanType(),
                transaction.getBillingCycle(),
                transaction.getCurrency(),
                transaction.getAmountMinor(),
                transaction.getAmountUsdMinor(),
                channels
        );
    }

    @Override
    public PaymentVerifyResponse verify(String reference) {
        assertPaystackConfigured();
        User currentUser = getAuthenticatedUser();

        PaymentTransaction transaction = paymentTransactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found: " + reference));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isAdmin && !transaction.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only verify your own payments");
        }

        verifyAndApply(reference);

        PaymentTransaction updated = paymentTransactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found after verification: " + reference));

        return toVerifyResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentHistoryItemResponse> getHistory() {
        User currentUser = getAuthenticatedUser();
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    @Override
    public void processWebhook(String signature, String payload) {
        assertPaystackConfigured();

        if (!isValidWebhookSignature(signature, payload)) {
            throw new AccessDeniedException("Invalid Paystack webhook signature");
        }

        JsonNode eventPayload = parseJson(payload);
        String event = readText(eventPayload, "event");
        JsonNode data = eventPayload.path("data");
        String reference = readText(data, "reference");

        if (isBlank(event) || isBlank(reference)) {
            return;
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByReference(reference).orElse(null);
        if (transaction == null) {
            return;
        }

        switch (event) {
            case "charge.success" -> verifyAndApply(reference);
            case "charge.failed" -> {
                if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                    transaction.setStatus(PaymentStatus.FAILED);
                    transaction.setGatewayStatus(readText(data, "status"));
                    transaction.setGatewayResponse(readText(data, "gateway_response"));
                    transaction.setChannel(readText(data, "channel"));
                    paymentTransactionRepository.save(transaction);
                }
            }
            case "charge.abandoned" -> {
                if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                    transaction.setStatus(PaymentStatus.ABANDONED);
                    transaction.setGatewayStatus(readText(data, "status"));
                    transaction.setGatewayResponse(readText(data, "gateway_response"));
                    transaction.setChannel(readText(data, "channel"));
                    paymentTransactionRepository.save(transaction);
                }
            }
            default -> {
                // Ignore unrelated events.
            }
        }
    }

    private void verifyAndApply(String reference) {
        JsonNode root = requestPaystack(
                "GET",
                "/transaction/verify/" + URLEncoder.encode(reference, StandardCharsets.UTF_8),
                null
        );
        JsonNode data = root.path("data");

        PaymentTransaction transaction = paymentTransactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found: " + reference));
        boolean wasAlreadySuccessful = transaction.getStatus() == PaymentStatus.SUCCESS;

        String gatewayStatus = readText(data, "status");
        String normalizedStatus = gatewayStatus == null ? "" : gatewayStatus.trim().toLowerCase();
        String gatewayResponse = readText(data, "gateway_response");
        String channel = readText(data, "channel");
        LocalDateTime paidAt = parsePaystackDateTime(readText(data, "paid_at"));
        long verifiedAmount = data.path("amount").asLong(-1);
        String verifiedCurrency = readText(data, "currency");

        transaction.setGatewayStatus(gatewayStatus);
        transaction.setGatewayResponse(gatewayResponse);
        transaction.setChannel(channel);

        if (verifiedAmount > 0 && !verifiedAmountEqualsExpected(verifiedAmount, transaction.getAmountMinor())) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setGatewayResponse("Amount mismatch during verification");
            paymentTransactionRepository.save(transaction);
            return;
        }

        if (!isBlank(verifiedCurrency) && !verifiedCurrency.equalsIgnoreCase(transaction.getCurrency().name())) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setGatewayResponse("Currency mismatch during verification");
            paymentTransactionRepository.save(transaction);
            return;
        }

        if ("success".equals(normalizedStatus)) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            if (paidAt != null) {
                transaction.setPaidAt(paidAt);
            } else if (transaction.getPaidAt() == null) {
                transaction.setPaidAt(LocalDateTime.now());
            }
            applySubscriptionForSuccessfulPayment(transaction);
            if (!wasAlreadySuccessful) {
                String portalPath = transaction.getUser().getRole() == UserRole.CANDIDATE
                        ? "/candidate/subscription"
                        : "/recruiter/subscription";
                notificationService.createForUser(
                        transaction.getUser().getId(),
                        NotificationType.PAYMENT_SUCCESS,
                        "Payment successful",
                        "Payment for " + transaction.getPlanType().name() + " (" + transaction.getBillingCycle().name() + ") completed successfully.",
                        portalPath
                );
            }
        } else if ("abandoned".equals(normalizedStatus)) {
            if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                transaction.setStatus(PaymentStatus.ABANDONED);
            }
        } else {
            if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                transaction.setStatus(PaymentStatus.FAILED);
            }
        }

        paymentTransactionRepository.save(transaction);
    }

    private void applySubscriptionForSuccessfulPayment(PaymentTransaction transaction) {
        Subscription subscription = subscriptionRepository.findByUserId(transaction.getUser().getId())
                .orElse(Subscription.builder().user(transaction.getUser()).build());

        PlanLimits limits = resolvePlanLimits(transaction.getPlanType(), resolvePricingRole(transaction.getUser()));
        LocalDateTime startDate = transaction.getPaidAt() == null ? LocalDateTime.now() : transaction.getPaidAt();
        LocalDateTime endDate = switch (transaction.getBillingCycle()) {
            case MONTHLY -> startDate.plusMonths(1);
            case ANNUAL -> startDate.plusYears(1);
        };

        subscription.setPlanType(transaction.getPlanType());
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setActive(true);
        subscription.setJobPostLimit(limits.jobPostLimit());
        subscription.setApplicantLimit(limits.applicantLimit());
        subscription.setApplicationLimit(limits.applicationLimit());
        subscription.setResumeScoreLimit(limits.resumeScoreLimit());
        subscription.setApplicationUsed(0);
        subscription.setResumeScoreUsed(0);
        subscription.setPaymentReference(transaction.getReference());

        subscriptionRepository.save(subscription);
    }

    private PlanLimits resolvePlanLimits(PlanType planType, UserRole pricingRole) {
        if (pricingRole == UserRole.CANDIDATE) {
            return switch (planType) {
                case FREE -> new PlanLimits(1, 25, 20, 50);
                case BASIC -> new PlanLimits(3, 120, 80, 400);
                case PRO -> new PlanLimits(8, 500, 250, 1500);
                case ENTERPRISE -> new PlanLimits(null, null, null, null);
            };
        }

        return switch (planType) {
            case FREE -> new PlanLimits(3, 50, 10, 20);
            case BASIC -> new PlanLimits(15, 400, 35, 100);
            case PRO -> new PlanLimits(75, 3000, 180, 500);
            case ENTERPRISE -> new PlanLimits(null, null, null, null);
        };
    }

    private record PlanLimits(
            Integer jobPostLimit,
            Integer applicantLimit,
            Integer applicationLimit,
            Integer resumeScoreLimit
    ) {
    }

    private Long resolveUsdMinorAmount(PlanType planType, BillingCycle cycle, UserRole pricingRole) {
        Map<PlanType, Map<BillingCycle, Long>> source = pricingRole == UserRole.CANDIDATE
                ? CANDIDATE_PLAN_USD_PRICE_MINOR
                : RECRUITER_PLAN_USD_PRICE_MINOR;

        Map<BillingCycle, Long> byCycle = source.get(planType);
        if (byCycle == null) {
            return null;
        }
        return byCycle.get(cycle);
    }

    private UserRole resolvePricingRole(User user) {
        if (user == null) {
            return UserRole.RECRUITER;
        }
        return user.getRole() == UserRole.CANDIDATE ? UserRole.CANDIDATE : UserRole.RECRUITER;
    }

    private long convertUsdMinor(long usdMinor, PaymentCurrency currency) {
        if (currency == PaymentCurrency.USD) {
            return usdMinor;
        }

        BigDecimal rate = switch (currency) {
            case NGN -> ngnRate;
            case GHS -> ghsRate;
            case KES -> kesRate;
            case ZAR -> zarRate;
            case USD -> BigDecimal.ONE;
        };

        return BigDecimal.valueOf(usdMinor)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private List<String> resolveChannels(List<String> requestedChannels) {
        List<String> configured = parseConfiguredChannels();
        if (requestedChannels == null || requestedChannels.isEmpty()) {
            return configured;
        }

        Set<String> deduplicated = new LinkedHashSet<>();
        for (String channel : requestedChannels) {
            if (isBlank(channel)) {
                continue;
            }
            String normalized = channel.trim().toLowerCase();
            if (!SUPPORTED_CHANNELS.contains(normalized)) {
                throw new BadRequestException("Unsupported payment channel: " + channel);
            }
            deduplicated.add(normalized);
        }

        if (deduplicated.isEmpty()) {
            return configured;
        }

        return new ArrayList<>(deduplicated);
    }

    private List<String> parseConfiguredChannels() {
        String value = paystackChannels == null ? "" : paystackChannels;
        Set<String> configured = new LinkedHashSet<>();
        for (String channel : value.split(",")) {
            if (isBlank(channel)) {
                continue;
            }
            configured.add(channel.trim().toLowerCase());
        }

        configured.removeIf(channel -> !SUPPORTED_CHANNELS.contains(channel));
        if (configured.isEmpty()) {
            configured.addAll(SUPPORTED_CHANNELS);
        }

        return new ArrayList<>(configured);
    }

    private List<PaymentCurrency> resolveSupportedCurrencies() {
        String configuredText = paystackSupportedCurrencies == null ? "" : paystackSupportedCurrencies;
        Set<PaymentCurrency> configured = new LinkedHashSet<>();
        configured.add(resolveDefaultCurrency());

        for (String raw : configuredText.split(",")) {
            if (isBlank(raw)) {
                continue;
            }
            String normalized = raw.trim().toUpperCase();
            try {
                configured.add(PaymentCurrency.valueOf(normalized));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown values from env.
            }
        }

        if (configured.size() == 1 && configured.contains(resolveDefaultCurrency())) {
            configured.add(PaymentCurrency.NGN);
        }
        return new ArrayList<>(configured);
    }

    private void validateCurrencySupported(PaymentCurrency currency) {
        List<PaymentCurrency> supported = resolveSupportedCurrencies();
        if (!supported.contains(currency)) {
            throw new BadRequestException(
                    "Currency " + currency.name() + " is not enabled for this merchant. Supported: " +
                            supported.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("NGN")
            );
        }
    }

    private PaystackInitializeResult initializeTransactionWithAutoCurrency(
            User currentUser,
            PaymentInitializeRequest request,
            Long amountUsdMinor,
            PaymentCurrency preferredCurrency
    ) {
        List<String> channels = resolveChannels(request.channels());
        List<PaymentCurrency> attemptCurrencies = resolveAttemptCurrencies(preferredCurrency);
        BadRequestException lastFailure = null;

        for (PaymentCurrency currency : attemptCurrencies) {
            String reference = generateReference();
            long amountMinor = convertUsdMinor(amountUsdMinor, currency);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("email", currentUser.getEmail());
            payload.put("amount", amountMinor);
            payload.put("currency", currency.name());
            payload.put("reference", reference);
            payload.put("callback_url", resolveCallbackUrl(currentUser));

            ArrayNode channelArray = payload.putArray("channels");
            channels.forEach(channelArray::add);

            ObjectNode metadata = payload.putObject("metadata");
            metadata.put("userId", currentUser.getId());
            metadata.put("planType", request.planType().name());
            metadata.put("billingCycle", request.billingCycle().name());
            metadata.put("currency", currency.name());
            metadata.put("currencyRequested", preferredCurrency.name());
            metadata.put("currencyAutoDetectEnabled", paystackAutoDetectCurrency);

            try {
                JsonNode root = requestPaystack("POST", "/transaction/initialize", payload.toString());
                return new PaystackInitializeResult(root, currency, amountMinor);
            } catch (BadRequestException exception) {
                lastFailure = exception;
                if (paystackAutoDetectCurrency && isCurrencyUnsupportedError(exception.getMessage())) {
                    continue;
                }
                throw exception;
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new BadRequestException("Unable to initialize payment for available currencies");
    }

    private List<PaymentCurrency> resolveAttemptCurrencies(PaymentCurrency preferredCurrency) {
        LinkedHashSet<PaymentCurrency> ordered = new LinkedHashSet<>();
        ordered.add(preferredCurrency);
        ordered.addAll(resolveSupportedCurrencies());
        return new ArrayList<>(ordered);
    }

    private boolean isCurrencyUnsupportedError(String message) {
        if (isBlank(message)) {
            return false;
        }
        String normalized = message.trim().toLowerCase();
        return normalized.contains("currency not supported");
    }

    private PaymentCurrency resolveDefaultCurrency() {
        if (!isBlank(paystackDefaultCurrency)) {
            try {
                return PaymentCurrency.valueOf(paystackDefaultCurrency.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to USD.
            }
        }
        return PaymentCurrency.USD;
    }

    private record PaystackInitializeResult(
            JsonNode root,
            PaymentCurrency currency,
            long amountMinor
    ) {
    }

    private JsonNode requestPaystack(String method, String path, String payload) {
        try {
            String normalizedBaseUrl = paystackBaseUrl == null ? "https://api.paystack.co" : paystackBaseUrl.replaceAll("/+$", "");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl + path))
                    .header("Authorization", "Bearer " + paystackSecretKey)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30));

            if ("POST".equalsIgnoreCase(method)) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload == null ? "{}" : payload));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = parseJson(response.body());

            if (response.statusCode() >= 400 || !root.path("status").asBoolean(false)) {
                String message = readText(root, "message");
                if (isBlank(message)) {
                    message = "Paystack request failed";
                }
                throw new BadRequestException(message);
            }

            return root;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Paystack request interrupted");
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Unable to reach Paystack: " + exception.getMessage());
        }
    }

    private JsonNode parseJson(String payload) {
        try {
            return objectMapper.readTree(payload == null ? "{}" : payload);
        } catch (Exception exception) {
            throw new BadRequestException("Invalid JSON payload");
        }
    }

    private PaymentVerifyResponse toVerifyResponse(PaymentTransaction transaction) {
        return new PaymentVerifyResponse(
                transaction.getReference(),
                transaction.getStatus(),
                transaction.getGatewayStatus(),
                transaction.getGatewayResponse(),
                transaction.getChannel(),
                transaction.getPlanType(),
                transaction.getBillingCycle(),
                transaction.getCurrency(),
                transaction.getAmountMinor(),
                transaction.getAmountUsdMinor(),
                transaction.getPaidAt()
        );
    }

    private PaymentHistoryItemResponse toHistoryItem(PaymentTransaction transaction) {
        return new PaymentHistoryItemResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getPlanType(),
                transaction.getBillingCycle(),
                transaction.getCurrency(),
                transaction.getAmountMinor(),
                transaction.getAmountUsdMinor(),
                transaction.getStatus(),
                transaction.getGatewayStatus(),
                transaction.getGatewayResponse(),
                transaction.getChannel(),
                transaction.getAuthorizationUrl(),
                transaction.getCreatedAt(),
                transaction.getPaidAt()
        );
    }

    private boolean isValidWebhookSignature(String signature, String payload) {
        if (isBlank(signature)) {
            return false;
        }

        String secret = isBlank(paystackWebhookSecret) ? paystackSecretKey : paystackWebhookSecret;
        if (isBlank(secret)) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature.trim());
        } catch (Exception exception) {
            return false;
        }
    }

    private void assertPaystackConfigured() {
        if (isBlank(paystackSecretKey)) {
            throw new BadRequestException("Paystack is not configured. Set PAYSTACK_SECRET_KEY.");
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private User getAuthenticatedUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principal = authentication.getName();
        if (isBlank(principal) || "anonymousUser".equalsIgnoreCase(principal)) {
            return null;
        }

        return userRepository.findByEmail(principal).orElse(null);
    }

    private void assertPaymentAllowedRole(User user) {
        if (user.getRole() != UserRole.RECRUITER && user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.CANDIDATE) {
            throw new AccessDeniedException("Payments are available only for recruiter, candidate, and admin accounts");
        }
    }

    private String resolveCallbackUrl(User currentUser) {
        if (!isBlank(paystackCallbackUrl)) {
            return paystackCallbackUrl.trim();
        }

        String normalizedBase = (frontendPublicBaseUrl == null || frontendPublicBaseUrl.isBlank())
                ? "http://localhost:3000"
                : frontendPublicBaseUrl.replaceAll("/+$", "");

        String path = currentUser.getRole() == UserRole.CANDIDATE ? "/candidate/subscription" : "/recruiter/subscription";
        return normalizedBase + path;
    }

    private String generateReference() {
        return "TF-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private LocalDateTime parsePaystackDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean verifiedAmountEqualsExpected(long verifiedAmount, Long expectedAmount) {
        return expectedAmount != null && expectedAmount == verifiedAmount;
    }
}
