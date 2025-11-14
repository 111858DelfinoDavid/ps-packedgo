# Employee System Backend Implementation Summary

## Overview
Complete backend implementation for employee management and operations system. This allows event administrators to create employees, assign them to specific events, and enables employees to scan QR codes for ticket validation and consumption registration.

## Architecture

### Service Communication Flow
```
Frontend (Angular)
    ↓
auth-service (/auth/employee/login)
    ↓ (validates credentials via WebClient)
users-service (/api/internal/employees/validate)
    ↓ (returns employee data)
auth-service (generates JWT with EMPLOYEE role)
    ↓
Frontend (stores token, navigates to dashboard)
    ↓
users-service (/api/employee/*)
    → /assigned-events
    → /validate-ticket
    → /register-consumption
    → /stats
```

## Files Created

### Auth Service (5 files)

#### 1. EmployeeLoginRequest.java
**Location**: `auth-service/src/main/java/com/packed_go/auth_service/dto/request/`
- Simple DTO with email and password
- Validation: @NotBlank, @Email, @Size(min=6)

#### 2. ValidateEmployeeResponse.java
**Location**: `auth-service/src/main/java/com/packed_go/auth_service/dto/response/`
- Returns employee data from users-service validation
- Fields: id, email, username, document, adminId, isActive

#### 3. AuthController.java (MODIFIED)
**Added endpoint**: `POST /auth/employee/login`
- Accepts EmployeeLoginRequest
- Extracts IP address and User-Agent
- Calls authService.loginEmployee()
- Returns LoginResponse with JWT token

#### 4. AuthService.java (MODIFIED)
**Added method**: `LoginResponse loginEmployee(EmployeeLoginRequest, String, String)`

#### 5. AuthServiceImpl.java (MODIFIED)
**Implementation of loginEmployee()**:
1. Calls usersServiceClient.validateEmployee() to verify credentials
2. Checks if employee is active
3. Creates or retrieves AuthUser with EMPLOYEE role
4. Verifies account is not locked
5. Calls processSuccessfulLogin() to generate JWT and create session
6. Returns LoginResponse with token and user info

#### 6. UsersServiceClient.java (MODIFIED)
**Added method**: `ValidateEmployeeResponse validateEmployee(String email, String password)`

#### 7. UsersServiceClientImpl.java (MODIFIED)
**Implementation of validateEmployee()**:
- Makes POST request to `/api/internal/employees/validate`
- Sends email and password in request body
- Returns ValidateEmployeeResponse with employee data
- Throws RuntimeException on failure

### Users Service (7 files)

#### 1. Employee.java
**Location**: `users-service/src/main/java/com/packed_go/users_service/entity/`
- JPA Entity for employee data
- Fields: id, email, username, passwordHash, document, adminId, isActive, createdAt
- @ManyToMany relationship with Set<Long> assignedEventIds
- Helper methods: addEvent(), removeEvent(), hasAccessToEvent()

#### 2. EmployeeRepository.java
**Location**: `users-service/src/main/java/com/packed_go/users_service/repository/`
- JPA Repository extending JpaRepository<Employee, Long>
- Custom queries:
  - findByEmail()
  - findByDocument()
  - findByAdminId()
  - findByAdminIdAndIsActive()
  - findActiveEmployeesByEventId() - @Query with JOIN
  - existsByEmail()
  - existsByDocument()

#### 3. EmployeeDTO.java (MODIFIED)
**Location**: `users-service/src/main/java/com/packed_go/users_service/dto/`
**Added**: ValidateEmployeeResponse (with @Builder)
- Returns: id, email, username, document, adminId, isActive

#### 4. EmployeeService.java
**Location**: `users-service/src/main/java/com/packed_go/users_service/service/`
- Service layer with business logic
- Methods:
  - createEmployee() - validates uniqueness, min 1 event, encodes password
  - updateEmployee() - checks admin ownership
  - toggleEmployeeStatus() - activate/deactivate
  - deleteEmployee() - with admin verification
  - getEmployeesByAdmin() - lists all for admin
  - getEmployeeById() - with admin check
  - findByEmail() - for authentication
  - validatePassword() - BCrypt comparison
  - getAssignedEvents() - returns event list (TODO: integrate with event-service)
  - hasAccessToEvent() - permission check
  - mapToResponse() - entity to DTO

#### 5. AdminEmployeeController.java
**Location**: `users-service/src/main/java/com/packed_go/users_service/controller/`
**Base Path**: `/api/admin/employees`
- POST / - Create employee (adminId from JWT)
- GET / - List all admin's employees
- GET /{id} - Get employee details
- PUT /{id} - Update employee
- PATCH /{id}/toggle-status - Toggle active/inactive
- DELETE /{id} - Delete employee

#### 6. EmployeeController.java
**Location**: `users-service/src/main/java/com/packed_go/users_service/controller/`
**Base Path**: `/api/employee`
- GET /assigned-events - Returns employee's assigned events
- POST /validate-ticket - Validates ticket QR (checks event permission)
- POST /register-consumption - Registers consumption (checks event permission)
- GET /stats - Returns daily statistics (mock)

#### 7. InternalEmployeeController.java (NEW)
**Location**: `users-service/src/main/java/com/packed_go/users_service/controller/`
**Base Path**: `/api/internal/employees`
**Purpose**: Internal endpoints for service-to-service communication
- POST /validate - Validates employee credentials for auth-service
  - Accepts: Map with email and password
  - Finds employee by email
  - Validates password with EmployeeService.validatePassword()
  - Returns: ValidateEmployeeResponse
  - NOT exposed to external clients

## Database Schema

### employees table
```sql
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    document BIGINT UNIQUE,
    admin_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES auth_users(id)
);

CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_document ON employees(document);
CREATE INDEX idx_employees_admin_id ON employees(admin_id);
```

### employee_events table (join table)
```sql
CREATE TABLE employee_events (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE (employee_id, event_id)
);

CREATE INDEX idx_employee_events_employee_id ON employee_events(employee_id);
CREATE INDEX idx_employee_events_event_id ON employee_events(event_id);
```

## Security & Permissions

### JWT Token for Employees
- Role: "EMPLOYEE"
- Claims: userId (from auth_users), username, role, permissions
- Generated by: JwtTokenProvider in auth-service

### Role Permissions (to be added)
```sql
-- Add EMPLOYEE role permissions
INSERT INTO role_permissions (role, permission) VALUES
('EMPLOYEE', 'tickets:validate'),
('EMPLOYEE', 'tickets:read'),
('EMPLOYEE', 'consumptions:register'),
('EMPLOYEE', 'consumptions:read'),
('EMPLOYEE', 'events:read_assigned');
```

## Authentication Flow

### Employee Login
1. Frontend sends POST to `/auth/employee/login` with email/password
2. AuthController extracts IP and User-Agent
3. AuthService.loginEmployee() calls UsersServiceClient.validateEmployee()
4. UsersServiceClient makes POST to `/api/internal/employees/validate`
5. InternalEmployeeController finds employee and validates password
6. ValidateEmployeeResponse returned to auth-service
7. auth-service checks isActive = true
8. auth-service creates/finds AuthUser with EMPLOYEE role
9. processSuccessfulLogin() generates JWT with EMPLOYEE role
10. LoginResponse with token returned to frontend
11. Frontend stores token and navigates to /employee/dashboard

### Employee Operations
1. Frontend sends request to `/api/employee/*` with JWT in Authorization header
2. Spring Security validates JWT and extracts employeeId from claims
3. Controller extracts employeeId from Authentication object
4. For event-specific operations (validate-ticket, register-consumption):
   - Controller calls employeeService.hasAccessToEvent(employeeId, eventId)
   - Returns 400 if employee doesn't have access to event
   - Proceeds with operation if authorized
5. Returns ApiResponse with success/error message

## Admin Operations

### Create Employee
1. Admin logs in, gets JWT with ADMIN role
2. Admin sends POST to `/api/admin/employees` with CreateEmployeeRequest
3. AdminEmployeeController extracts adminId from JWT
4. EmployeeService.createEmployee() validates:
   - Email/document uniqueness
   - At least 1 assigned event
5. Password is hashed with BCryptPasswordEncoder
6. Employee entity saved with adminId
7. Returns EmployeeResponse with created employee data

### Update Employee
1. Admin sends PUT to `/api/admin/employees/{id}` with UpdateEmployeeRequest
2. EmployeeService.updateEmployee() validates:
   - Admin ownership (employee.adminId == adminId from JWT)
   - Email/document uniqueness if changed
   - At least 1 assigned event
3. Employee entity updated
4. Returns EmployeeResponse with updated data

### Toggle Status / Delete
- Similar ownership checks
- toggleEmployeeStatus() sets isActive = !isActive
- deleteEmployee() removes employee from database

## Mock Implementations (TODO)

### Event Details Integration
**Current**: EmployeeService.getAssignedEvents() returns placeholder names
**TODO**: Integrate with event-service
- Add RestTemplate or WebClient call to event-service
- Endpoint: GET /events/batch?ids=1,2,3
- Returns full event details (name, location, date, status)

### Ticket Validation
**Current**: EmployeeController.validateTicket() returns mock response
**TODO**: Integrate with ticket-service or event-service
- Verify ticket exists and belongs to event
- Check if ticket already used
- Update ticket status to used
- Return real customer name, ticket type, seat number

### Consumption Registration
**Current**: EmployeeController.registerConsumption() returns mock response
**TODO**: Integrate with consumption-service or order-service
- Verify consumption code exists
- Check customer has available balance
- Deduct from prepaid consumptions if applicable
- Record consumption with employee_id and timestamp
- Return real item name, quantity

### Statistics
**Current**: EmployeeController.getStats() returns mock data (15/23/38)
**TODO**: Real implementation
- Query ticket_validations table: COUNT(*) WHERE employee_id = ? AND DATE(validated_at) = CURRENT_DATE
- Query consumption_registrations table: COUNT(*) WHERE employee_id = ? AND DATE(registered_at) = CURRENT_DATE
- Return real counts

## Testing Checklist

### Authentication
- [ ] Employee can login with valid email/password
- [ ] Login fails with invalid email
- [ ] Login fails with invalid password
- [ ] Login fails if employee is inactive (isActive = false)
- [ ] JWT token contains EMPLOYEE role
- [ ] JWT token contains correct permissions

### Admin Operations
- [ ] Admin can create employee with multiple events
- [ ] Creation fails without assigned events
- [ ] Creation fails with duplicate email
- [ ] Creation fails with duplicate document
- [ ] Admin can only see their own employees
- [ ] Admin can update employee's assigned events
- [ ] Admin can toggle employee active status
- [ ] Admin can delete employee
- [ ] Admin cannot modify another admin's employees

### Employee Operations
- [ ] Employee can see assigned events
- [ ] Employee can validate ticket for assigned event
- [ ] Ticket validation fails for unassigned event
- [ ] Employee can register consumption for assigned event
- [ ] Consumption registration fails for unassigned event
- [ ] Employee can view their statistics
- [ ] Unauthorized access returns 401/403

### Database
- [ ] Employees table created
- [ ] Employee_events join table created
- [ ] Foreign key constraints working
- [ ] Unique constraints enforced (email, document)
- [ ] Cascade delete works (deleting employee removes assignments)

## Configuration

### Application Properties (users-service)
```properties
# Already configured in docker-compose
spring.datasource.url=jdbc:postgresql://postgres:5432/packedgo_users
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
```

### WebClient Configuration (auth-service)
```java
@Bean
@Qualifier("usersServiceWebClient")
public WebClient usersServiceWebClient() {
    return WebClient.builder()
        .baseUrl("http://users-service:8081") // Docker service name
        .build();
}
```

## Next Steps

1. **Database Migration**: Create SQL migration script for employees and employee_events tables
2. **Role Permissions**: Add EMPLOYEE role to role_permissions table
3. **Integration Testing**: Test complete flow from admin creation to employee scanning
4. **Event Service Integration**: Replace placeholder event names with real data
5. **Ticket/Consumption Integration**: Implement real validation and registration logic
6. **Statistics**: Implement real counting from database tables
7. **Frontend Testing**: Test all Angular components with real backend
8. **Documentation**: Add API documentation (Swagger/OpenAPI)

## API Endpoints Summary

### Auth Service
- `POST /auth/employee/login` - Employee authentication

### Users Service - Admin
- `POST /api/admin/employees` - Create employee
- `GET /api/admin/employees` - List admin's employees
- `GET /api/admin/employees/{id}` - Get employee details
- `PUT /api/admin/employees/{id}` - Update employee
- `PATCH /api/admin/employees/{id}/toggle-status` - Toggle active status
- `DELETE /api/admin/employees/{id}` - Delete employee

### Users Service - Employee
- `GET /api/employee/assigned-events` - Get assigned events
- `POST /api/employee/validate-ticket` - Validate ticket QR
- `POST /api/employee/register-consumption` - Register consumption QR
- `GET /api/employee/stats` - Get daily statistics

### Users Service - Internal
- `POST /api/internal/employees/validate` - Validate employee credentials (internal only)

## Notes

- All passwords are hashed using BCryptPasswordEncoder
- Employee accounts are created by admins with isActive = true by default
- Email verification is not required for employees (they're created by trusted admins)
- Employees can only access events they're explicitly assigned to
- Admin ownership is verified on all admin operations
- JWT tokens are stored in UserSession table for session management
- Login attempts are recorded in LoginAttempt table for security auditing
