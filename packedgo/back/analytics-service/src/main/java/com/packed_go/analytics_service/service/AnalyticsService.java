package com.packed_go.analytics_service.service;

import com.packed_go.analytics_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Servicio de Analytics que consume datos de otros microservicios
 * y calcula métricas agregadas para el dashboard
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.services.event-service.base-url:http://localhost:8086}")
    private String eventServiceUrl;

    @Value("${app.services.order-service.base-url:http://localhost:8084}")
    private String orderServiceUrl;

    @Value("${app.services.payment-service.base-url:http://localhost:8085}")
    private String paymentServiceUrl;

    /**
     * Genera el dashboard completo de analytics para un organizador
     */
    public DashboardDTO generateDashboard(Long organizerId, String authToken) {
        log.info("📊 Generando dashboard para organizador: {}", organizerId);

        try {
            // Obtener datos de los microservicios
            List<Map<String, Object>> events = fetchOrganizerEvents(organizerId, authToken);
            List<Map<String, Object>> orders = fetchOrganizerOrders(organizerId, authToken);
            List<Map<String, Object>> consumptions = fetchOrganizerConsumptions(organizerId, authToken);

            // Calcular métricas
            SalesMetricsDTO salesMetrics = calculateSalesMetrics(orders);
            EventMetricsDTO eventMetrics = calculateEventMetrics(events);
            ConsumptionMetricsDTO consumptionMetrics = calculateConsumptionMetrics(consumptions, orders);
            RevenueMetricsDTO revenueMetrics = calculateRevenueMetrics(orders);
            TopPerformersDTO topPerformers = calculateTopPerformers(events, consumptions, orders);
            TrendsDTO trends = calculateTrends(orders);

            return DashboardDTO.builder()
                    .organizerId(organizerId)
                    .organizerName("Organizador " + organizerId) // TODO: Obtener nombre real del Users-Service
                    .lastUpdated(LocalDateTime.now())
                    .salesMetrics(salesMetrics)
                    .eventMetrics(eventMetrics)
                    .consumptionMetrics(consumptionMetrics)
                    .revenueMetrics(revenueMetrics)
                    .topPerformers(topPerformers)
                    .trends(trends)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error generando dashboard: {}", e.getMessage(), e);
            return generateEmptyDashboard(organizerId);
        }
    }

    // ==================== FETCH DATA FROM MICROSERVICES ====================

    private List<Map<String, Object>> fetchOrganizerEvents(Long organizerId, String authToken) {
        try {
            String url = eventServiceUrl + "/api/events/my-events";
            log.info("📡 Fetching events from: {}", url);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error fetching events: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchOrganizerOrders(Long organizerId, String authToken) {
        try {
            String url = orderServiceUrl + "/api/orders/organizer/" + organizerId;
            log.info("📡 Fetching orders from: {}", url);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error fetching orders: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> fetchOrganizerConsumptions(Long organizerId, String authToken) {
        try {
            String url = eventServiceUrl + "/api/consumptions/my-consumptions";
            log.info("📡 Fetching consumptions from: {}", url);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error fetching consumptions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== CALCULATE METRICS ====================

    private SalesMetricsDTO calculateSalesMetrics(List<Map<String, Object>> orders) {
        Long totalOrders = (long) orders.size();
        Long paidOrders = orders.stream().filter(o -> "PAID".equals(o.get("status"))).count();
        Long pendingOrders = orders.stream().filter(o -> "PENDING_PAYMENT".equals(o.get("status"))).count();
        Long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equals(o.get("status"))).count();

        // Total tickets vendidos (suma de items en órdenes pagadas)
        Long totalTicketsSold = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .mapToLong(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.size() : 0L;
                })
                .sum();

        // Tickets vendidos hoy
        LocalDate today = LocalDate.now();
        Long ticketsSoldToday = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    return createdAt != null && createdAt.startsWith(today.toString());
                })
                .mapToLong(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.size() : 0L;
                })
                .sum();

        // Tickets vendidos esta semana (últimos 7 días)
        LocalDate weekAgo = today.minusDays(7);
        Long ticketsSoldThisWeek = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                    return orderDate.isAfter(weekAgo) || orderDate.isEqual(weekAgo);
                })
                .mapToLong(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.size() : 0L;
                })
                .sum();

        // Tickets vendidos este mes
        LocalDate monthStart = today.withDayOfMonth(1);
        Long ticketsSoldThisMonth = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                    return orderDate.isAfter(monthStart) || orderDate.isEqual(monthStart);
                })
                .mapToLong(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.size() : 0L;
                })
                .sum();

        // Tasa de conversión
        Double conversionRate = totalOrders > 0 ? (paidOrders.doubleValue() / totalOrders) * 100 : 0.0;

        // Valor promedio de orden
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageOrderValue = paidOrders > 0 
                ? totalRevenue.divide(new BigDecimal(paidOrders), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        return SalesMetricsDTO.builder()
                .totalTicketsSold(totalTicketsSold)
                .ticketsSoldToday(ticketsSoldToday)
                .ticketsSoldThisWeek(ticketsSoldThisWeek)
                .ticketsSoldThisMonth(ticketsSoldThisMonth)
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    private EventMetricsDTO calculateEventMetrics(List<Map<String, Object>> events) {
        Long totalEvents = (long) events.size();
        Long activeEvents = events.stream().filter(e -> "ACTIVE".equals(e.get("status"))).count();
        Long completedEvents = events.stream().filter(e -> "COMPLETED".equals(e.get("status"))).count();
        Long cancelledEvents = events.stream().filter(e -> "CANCELLED".equals(e.get("status"))).count();
        
        // Eventos futuros (eventDate > hoy)
        LocalDateTime now = LocalDateTime.now();
        Long upcomingEvents = events.stream()
                .filter(e -> {
                    String eventDate = (String) e.get("eventDate");
                    return eventDate != null && LocalDateTime.parse(eventDate).isAfter(now);
                })
                .count();

        // Capacidad
        Long totalCapacity = events.stream()
                .mapToLong(e -> e.get("maxCapacity") != null ? ((Number) e.get("maxCapacity")).longValue() : 0L)
                .sum();
        
        Long occupiedCapacity = events.stream()
                .mapToLong(e -> e.get("soldPasses") != null ? ((Number) e.get("soldPasses")).longValue() : 0L)
                .sum();

        Double averageOccupancyRate = totalCapacity > 0 
                ? (occupiedCapacity.doubleValue() / totalCapacity) * 100 
                : 0.0;

        // Evento más vendido
        Map<String, Object> mostSoldEvent = events.stream()
                .max(Comparator.comparingLong(e -> e.get("soldPasses") != null ? ((Number) e.get("soldPasses")).longValue() : 0L))
                .orElse(null);

        return EventMetricsDTO.builder()
                .totalEvents(totalEvents)
                .activeEvents(activeEvents)
                .completedEvents(completedEvents)
                .cancelledEvents(cancelledEvents)
                .upcomingEvents(upcomingEvents)
                .totalCapacity(totalCapacity)
                .occupiedCapacity(occupiedCapacity)
                .averageOccupancyRate(Math.round(averageOccupancyRate * 100.0) / 100.0)
                .mostSoldEventId(mostSoldEvent != null ? ((Number) mostSoldEvent.get("id")).longValue() : null)
                .mostSoldEventName(mostSoldEvent != null ? (String) mostSoldEvent.get("name") : "N/A")
                .mostSoldEventTickets(mostSoldEvent != null && mostSoldEvent.get("soldPasses") != null 
                        ? ((Number) mostSoldEvent.get("soldPasses")).longValue() : 0L)
                .build();
    }

    private ConsumptionMetricsDTO calculateConsumptionMetrics(List<Map<String, Object>> consumptions, List<Map<String, Object>> orders) {
        Long totalConsumptions = (long) consumptions.size();
        Long activeConsumptions = consumptions.stream().filter(c -> Boolean.TRUE.equals(c.get("active"))).count();

        // Consumiciones vendidas (contar en items de órdenes pagadas)
        Long totalConsumptionsSold = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .flatMap(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.stream() : Stream.empty();
                })
                .flatMap(item -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> itemConsumptions = (List<Map<String, Object>>) item.get("consumptions");
                    return itemConsumptions != null ? itemConsumptions.stream() : Stream.empty();
                })
                .count();

        // Para tasas de canje, necesitaríamos datos del consumption-service (QR canjeados)
        // Por ahora, simulamos con valores por defecto
        Long consumptionsRedeemed = totalConsumptionsSold / 2; // Simulación: 50% canjeados
        Long consumptionsPending = totalConsumptionsSold - consumptionsRedeemed;
        Double redemptionRate = totalConsumptionsSold > 0 
                ? (consumptionsRedeemed.doubleValue() / totalConsumptionsSold) * 100 
                : 0.0;

        // Consumición más vendida (requiere contar por tipo)
        Map<Long, Long> consumptionCount = new HashMap<>();
        orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .flatMap(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.stream() : Stream.empty();
                })
                .flatMap(item -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> itemConsumptions = (List<Map<String, Object>>) item.get("consumptions");
                    return itemConsumptions != null ? itemConsumptions.stream() : Stream.empty();
                })
                .forEach(ic -> {
                    Long consumptionId = ((Number) ic.get("consumptionId")).longValue();
                    consumptionCount.put(consumptionId, consumptionCount.getOrDefault(consumptionId, 0L) + 1);
                });

        Long mostSoldConsumptionId = consumptionCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String mostSoldConsumptionName = "N/A";
        Long mostSoldConsumptionQuantity = 0L;
        if (mostSoldConsumptionId != null) {
            mostSoldConsumptionQuantity = consumptionCount.get(mostSoldConsumptionId);
            Optional<Map<String, Object>> consumption = consumptions.stream()
                    .filter(c -> mostSoldConsumptionId.equals(((Number) c.get("id")).longValue()))
                    .findFirst();
            mostSoldConsumptionName = consumption.map(c -> (String) c.get("name")).orElse("N/A");
        }

        return ConsumptionMetricsDTO.builder()
                .totalConsumptions(totalConsumptions)
                .activeConsumptions(activeConsumptions)
                .totalConsumptionsSold(totalConsumptionsSold)
                .consumptionsRedeemed(consumptionsRedeemed)
                .consumptionsPending(consumptionsPending)
                .redemptionRate(Math.round(redemptionRate * 100.0) / 100.0)
                .mostSoldConsumptionId(mostSoldConsumptionId)
                .mostSoldConsumptionName(mostSoldConsumptionName)
                .mostSoldConsumptionQuantity(mostSoldConsumptionQuantity)
                .build();
    }

    private RevenueMetricsDTO calculateRevenueMetrics(List<Map<String, Object>> orders) {
        List<Map<String, Object>> paidOrders = orders.stream()
                .filter(o -> "PAID".equals(o.get("status")))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = paidOrders.stream()
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue hoy
        LocalDate today = LocalDate.now();
        BigDecimal revenueToday = paidOrders.stream()
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    return createdAt != null && createdAt.startsWith(today.toString());
                })
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue esta semana
        LocalDate weekAgo = today.minusDays(7);
        BigDecimal revenueThisWeek = paidOrders.stream()
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                    return orderDate.isAfter(weekAgo) || orderDate.isEqual(weekAgo);
                })
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue este mes
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal revenueThisMonth = paidOrders.stream()
                .filter(o -> {
                    String createdAt = (String) o.get("createdAt");
                    if (createdAt == null) return false;
                    LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                    return orderDate.isAfter(monthStart) || orderDate.isEqual(monthStart);
                })
                .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Separar ingresos por tipo (requiere lógica adicional)
        // Por ahora, asumimos 70% de tickets y 30% de consumiciones
        BigDecimal revenueFromTickets = totalRevenue.multiply(new BigDecimal("0.70"));
        BigDecimal revenueFromConsumptions = totalRevenue.multiply(new BigDecimal("0.30"));

        // Promedio por evento y por cliente (requiere contar eventos únicos y clientes únicos)
        Long uniqueEvents = paidOrders.stream()
                .flatMap(o -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) o.get("items");
                    return items != null ? items.stream() : Stream.empty();
                })
                .map(item -> ((Number) item.get("eventId")).longValue())
                .distinct()
                .count();

        Long uniqueCustomers = paidOrders.stream()
                .map(o -> ((Number) o.get("userId")).longValue())
                .distinct()
                .count();

        BigDecimal averageRevenuePerEvent = uniqueEvents > 0 
                ? totalRevenue.divide(new BigDecimal(uniqueEvents), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        BigDecimal averageRevenuePerCustomer = uniqueCustomers > 0 
                ? totalRevenue.divide(new BigDecimal(uniqueCustomers), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        return RevenueMetricsDTO.builder()
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .revenueFromTickets(revenueFromTickets)
                .revenueFromConsumptions(revenueFromConsumptions)
                .averageRevenuePerEvent(averageRevenuePerEvent)
                .averageRevenuePerCustomer(averageRevenuePerCustomer)
                .build();
    }

    private TopPerformersDTO calculateTopPerformers(List<Map<String, Object>> events, 
                                                     List<Map<String, Object>> consumptions, 
                                                     List<Map<String, Object>> orders) {
        // Top 5 eventos más vendidos
        List<EventPerformanceDTO> topEvents = events.stream()
                .sorted((e1, e2) -> {
                    Long sold1 = e1.get("soldPasses") != null ? ((Number) e1.get("soldPasses")).longValue() : 0L;
                    Long sold2 = e2.get("soldPasses") != null ? ((Number) e2.get("soldPasses")).longValue() : 0L;
                    return sold2.compareTo(sold1);
                })
                .limit(5)
                .map(e -> EventPerformanceDTO.builder()
                        .eventId(((Number) e.get("id")).longValue())
                        .eventName((String) e.get("name"))
                        .ticketsSold(e.get("soldPasses") != null ? ((Number) e.get("soldPasses")).longValue() : 0L)
                        .revenue(new BigDecimal(e.get("basePrice").toString()).multiply(
                                new BigDecimal(e.get("soldPasses") != null ? e.get("soldPasses").toString() : "0")))
                        .occupancyRate(calculateOccupancyRate(e))
                        .build())
                .collect(Collectors.toList());

        // Top 5 consumiciones (simplificado)
        List<ConsumptionPerformanceDTO> topConsumptions = consumptions.stream()
                .limit(5)
                .map(c -> ConsumptionPerformanceDTO.builder()
                        .consumptionId(((Number) c.get("id")).longValue())
                        .consumptionName((String) c.get("name"))
                        .quantitySold(0L) // TODO: Calcular desde órdenes
                        .revenue(BigDecimal.ZERO) // TODO: Calcular desde órdenes
                        .redemptionRate(0.0) // TODO: Calcular desde consumption-service
                        .build())
                .collect(Collectors.toList());

        // Categorías vacías por ahora
        List<CategoryPerformanceDTO> topEventCategories = new ArrayList<>();
        List<CategoryPerformanceDTO> topConsumptionCategories = new ArrayList<>();

        return TopPerformersDTO.builder()
                .topEvents(topEvents)
                .topConsumptions(topConsumptions)
                .topEventCategories(topEventCategories)
                .topConsumptionCategories(topConsumptionCategories)
                .build();
    }

    private Double calculateOccupancyRate(Map<String, Object> event) {
        Object maxCap = event.get("maxCapacity");
        Object sold = event.get("soldPasses");
        
        if (maxCap == null || sold == null) return 0.0;
        
        Long maxCapacity = ((Number) maxCap).longValue();
        Long soldPasses = ((Number) sold).longValue();
        
        return maxCapacity > 0 ? (soldPasses.doubleValue() / maxCapacity) * 100 : 0.0;
    }

    private TrendsDTO calculateTrends(List<Map<String, Object>> orders) {
        // Tendencias diarias (últimos 30 días)
        List<DailyTrendDTO> dailySales = generateDailyTrends(orders, 30);
        List<DailyTrendDTO> dailyRevenue = generateDailyRevenueTrends(orders, 30);

        // Tendencias mensuales (último año)
        List<MonthlyTrendDTO> monthlySales = generateMonthlyTrends(orders, 12);
        List<MonthlyTrendDTO> monthlyRevenue = generateMonthlyRevenueTrends(orders, 12);

        return TrendsDTO.builder()
                .dailySales(dailySales)
                .dailyRevenue(dailyRevenue)
                .monthlySales(monthlySales)
                .monthlyRevenue(monthlyRevenue)
                .build();
    }

    private List<DailyTrendDTO> generateDailyTrends(List<Map<String, Object>> orders, int days) {
        LocalDate today = LocalDate.now();
        List<DailyTrendDTO> trends = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Long count = orders.stream()
                    .filter(o -> "PAID".equals(o.get("status")))
                    .filter(o -> {
                        String createdAt = (String) o.get("createdAt");
                        return createdAt != null && createdAt.startsWith(date.toString());
                    })
                    .count();

            BigDecimal amount = orders.stream()
                    .filter(o -> "PAID".equals(o.get("status")))
                    .filter(o -> {
                        String createdAt = (String) o.get("createdAt");
                        return createdAt != null && createdAt.startsWith(date.toString());
                    })
                    .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trends.add(DailyTrendDTO.builder()
                    .date(date)
                    .count(count)
                    .amount(amount)
                    .build());
        }

        return trends;
    }

    private List<DailyTrendDTO> generateDailyRevenueTrends(List<Map<String, Object>> orders, int days) {
        return generateDailyTrends(orders, days); // Misma lógica
    }

    private List<MonthlyTrendDTO> generateMonthlyTrends(List<Map<String, Object>> orders, int months) {
        LocalDate today = LocalDate.now();
        List<MonthlyTrendDTO> trends = new ArrayList<>();
        String[] monthNames = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        for (int i = months - 1; i >= 0; i--) {
            LocalDate targetMonth = today.minusMonths(i);
            int year = targetMonth.getYear();
            int month = targetMonth.getMonthValue();

            Long count = orders.stream()
                    .filter(o -> "PAID".equals(o.get("status")))
                    .filter(o -> {
                        String createdAt = (String) o.get("createdAt");
                        if (createdAt == null) return false;
                        LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                        return orderDate.getYear() == year && orderDate.getMonthValue() == month;
                    })
                    .count();

            BigDecimal amount = orders.stream()
                    .filter(o -> "PAID".equals(o.get("status")))
                    .filter(o -> {
                        String createdAt = (String) o.get("createdAt");
                        if (createdAt == null) return false;
                        LocalDate orderDate = LocalDate.parse(createdAt.substring(0, 10));
                        return orderDate.getYear() == year && orderDate.getMonthValue() == month;
                    })
                    .map(o -> new BigDecimal(o.get("totalAmount").toString()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trends.add(MonthlyTrendDTO.builder()
                    .year(year)
                    .month(month)
                    .monthName(monthNames[month - 1])
                    .count(count)
                    .amount(amount)
                    .build());
        }

        return trends;
    }

    private List<MonthlyTrendDTO> generateMonthlyRevenueTrends(List<Map<String, Object>> orders, int months) {
        return generateMonthlyTrends(orders, months); // Misma lógica
    }

    private DashboardDTO generateEmptyDashboard(Long organizerId) {
        return DashboardDTO.builder()
                .organizerId(organizerId)
                .organizerName("Organizador " + organizerId)
                .lastUpdated(LocalDateTime.now())
                .salesMetrics(SalesMetricsDTO.builder().build())
                .eventMetrics(EventMetricsDTO.builder().build())
                .consumptionMetrics(ConsumptionMetricsDTO.builder().build())
                .revenueMetrics(RevenueMetricsDTO.builder().build())
                .topPerformers(TopPerformersDTO.builder()
                        .topEvents(new ArrayList<>())
                        .topConsumptions(new ArrayList<>())
                        .topEventCategories(new ArrayList<>())
                        .topConsumptionCategories(new ArrayList<>())
                        .build())
                .trends(TrendsDTO.builder()
                        .dailySales(new ArrayList<>())
                        .dailyRevenue(new ArrayList<>())
                        .monthlySales(new ArrayList<>())
                        .monthlyRevenue(new ArrayList<>())
                        .build())
                .build();
    }
}
