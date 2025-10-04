// ============================================
// ADMIN EVENTS MANAGEMENT - JAVASCRIPT
// ============================================

// ============================================
// CONFIGURACIÓN Y UTILIDADES
// ============================================
const API_BASE_URL = 'http://localhost:8086/api/event-service';

// JWT Utils
const JWTUtils = {
    decodeToken(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error('Error decoding token:', error);
            return null;
        }
    },

    getUserId() {
        const token = localStorage.getItem('userToken');
        if (!token) return null;
        const decoded = this.decodeToken(token);
        return decoded ? decoded.userId : null;
    },

    getRole() {
        const token = localStorage.getItem('userToken');
        if (!token) return null;
        const decoded = this.decodeToken(token);
        return decoded ? decoded.role : null;
    }
};

// Verificar que el usuario sea admin
function checkAdminAccess() {
    const token = localStorage.getItem('userToken');
    
    if (!token) {
        alert('Acceso denegado. Por favor inicie sesión.');
        window.location.href = 'admin-login.html';
        return false;
    }
    
    // Intentar obtener el rol del token JWT
    const roleFromToken = JWTUtils.getRole();
    // Si el token no es un JWT válido, usar el rol almacenado (fallback)
    const storedRole = localStorage.getItem('userRole');
    const effectiveRole = roleFromToken || storedRole;
    
    if (!effectiveRole || (effectiveRole !== 'ADMIN' && effectiveRole !== 'SUPER_ADMIN')) {
        alert('Acceso denegado. Solo administradores pueden acceder a esta página.');
        window.location.href = 'admin-login.html';
        return false;
    }
    
    return true;
}

// Mostrar alerta
function showAlert(message, type, containerId) {
    const alertHtml = `
        <div class="alert alert-${type} alert-dismissible fade show alert-custom" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    document.getElementById(containerId).innerHTML = alertHtml;
    setTimeout(() => {
        const alertElement = document.querySelector(`#${containerId} .alert`);
        if (alertElement) {
            alertElement.remove();
        }
    }, 5000);
}

// Logout
function logout() {
    localStorage.removeItem('userToken');
    window.location.href = 'admin-login.html';
}

// ============================================
// EVENTOS
// ============================================
let currentEventId = null;
let eventCategories = [];

async function loadEvents() {
    try {
        const response = await fetch(`${API_BASE_URL}/event`);
        if (!response.ok) throw new Error('Error al cargar eventos');
        
        const events = await response.json();
        renderEvents(events);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('eventsTableBody').innerHTML = `
            <tr><td colspan="9" class="text-center text-danger">Error al cargar eventos</td></tr>
        `;
    }
}

function renderEvents(events) {
    const tbody = document.getElementById('eventsTableBody');
    
    if (events.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="empty-state">
                        <i class="bi bi-calendar-x"></i>
                        <p>No hay eventos registrados</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = events.map(event => `
        <tr>
            <td>${event.id}</td>
            <td>
                ${event.imageUrl ? 
                    `<img src="${event.imageUrl}" class="event-image-thumb" alt="${event.name}">` :
                    '<div class="event-image-thumb bg-secondary d-flex align-items-center justify-content-center"><i class="bi bi-image text-white"></i></div>'
                }
            </td>
            <td><strong>${event.name}</strong></td>
            <td>${event.categoryId || 'N/A'}</td>
            <td>${new Date(event.eventDate).toLocaleDateString('es-ES', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'})}</td>
            <td>${event.currentCapacity || 0} / ${event.maxCapacity}</td>
            <td>$${parseFloat(event.basePrice).toFixed(2)}</td>
            <td><span class="badge-status ${event.active ? 'badge-active' : 'badge-inactive'}">${event.active ? 'Activo' : 'Inactivo'}</span></td>
            <td class="action-buttons">
                <button class="btn btn-sm btn-primary" onclick="editEvent(${event.id})" title="Editar">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteEvent(${event.id})" title="Eliminar">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

async function loadEventCategories() {
    try {
        const response = await fetch(`${API_BASE_URL}/category`);
        if (!response.ok) throw new Error('Error al cargar categorías');
        
        eventCategories = await response.json();
        
        // Llenar select del modal
        const select = document.getElementById('eventCategoryId');
        select.innerHTML = '<option value="">Seleccione una categoría</option>' +
            eventCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
        
        // Renderizar cards de categorías
        renderEventCategories(eventCategories);
    } catch (error) {
        console.error('Error:', error);
    }
}

function renderEventCategories(categories) {
    const container = document.getElementById('eventCategoriesContainer');
    
    if (categories.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="bi bi-tags"></i>
                    <p>No hay categorías de eventos registradas</p>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = categories.map(category => `
        <div class="col-md-4 col-lg-3 mb-3">
            <div class="card-custom text-center">
                <i class="bi bi-tag-fill" style="font-size: 3rem; color: #667eea;"></i>
                <h5 class="mt-3 mb-2">${category.name}</h5>
                <span class="badge-status ${category.active ? 'badge-active' : 'badge-inactive'} mb-3">
                    ${category.active ? 'Activa' : 'Inactiva'}
                </span>
                <div class="d-flex justify-content-center gap-2">
                    <button class="btn btn-sm btn-primary" onclick="editEventCategory(${category.id}, '${category.name}')">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-warning" onclick="toggleEventCategoryStatus(${category.id})">
                        <i class="bi bi-toggle-on"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteEventCategory(${category.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function openCreateEventModal() {
    currentEventId = null;
    document.getElementById('eventModalTitle').innerHTML = '<i class="bi bi-calendar-event"></i> Crear Nuevo Evento';
    document.getElementById('eventForm').reset();
    document.getElementById('eventId').value = '';
    new bootstrap.Modal(document.getElementById('eventModal')).show();
}

async function editEvent(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/event/${id}`);
        if (!response.ok) throw new Error('Error al cargar evento');
        
        const event = await response.json();
        currentEventId = id;
        
        document.getElementById('eventModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Evento';
        document.getElementById('eventId').value = event.id;
        document.getElementById('eventName').value = event.name;
        document.getElementById('eventCategoryId').value = event.categoryId;
        document.getElementById('eventDescription').value = event.description;
        document.getElementById('eventDate').value = event.eventDate.substring(0, 16);
        document.getElementById('eventBasePrice').value = event.basePrice;
        document.getElementById('eventMaxCapacity').value = event.maxCapacity;
        document.getElementById('eventStatus').value = event.status;
        document.getElementById('eventLat').value = event.lat;
        document.getElementById('eventLng').value = event.lng;
        document.getElementById('eventImageUrl').value = event.imageUrl || '';
        
        new bootstrap.Modal(document.getElementById('eventModal')).show();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar los datos del evento', 'danger', 'eventsAlert');
    }
}

async function saveEvent() {
    const userId = JWTUtils.getUserId();
    const eventData = {
        categoryId: parseInt(document.getElementById('eventCategoryId').value),
        name: document.getElementById('eventName').value,
        description: document.getElementById('eventDescription').value,
        eventDate: document.getElementById('eventDate').value,
        lat: parseFloat(document.getElementById('eventLat').value),
        lng: parseFloat(document.getElementById('eventLng').value),
        maxCapacity: parseInt(document.getElementById('eventMaxCapacity').value),
        currentCapacity: 0,
        basePrice: parseFloat(document.getElementById('eventBasePrice').value),
        imageUrl: document.getElementById('eventImageUrl').value || null,
        status: document.getElementById('eventStatus').value,
        createdBy: userId,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    };

    try {
        const token = localStorage.getItem('userToken');
        const url = currentEventId ? `${API_BASE_URL}/event/${currentEventId}` : `${API_BASE_URL}/event`;
        const method = currentEventId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(eventData)
        });

        if (!response.ok) throw new Error('Error al guardar evento');

        bootstrap.Modal.getInstance(document.getElementById('eventModal')).hide();
        showAlert(`Evento ${currentEventId ? 'actualizado' : 'creado'} exitosamente`, 'success', 'eventsAlert');
        loadEvents();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar el evento', 'danger', 'eventsAlert');
    }
}

async function deleteEvent(id) {
    if (!confirm('¿Está seguro de eliminar este evento?')) return;

    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/event/logical/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al eliminar evento');

        showAlert('Evento eliminado exitosamente', 'success', 'eventsAlert');
        loadEvents();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar el evento', 'danger', 'eventsAlert');
    }
}

// ============================================
// CATEGORÍAS DE EVENTOS
// ============================================
function openCreateEventCategoryModal() {
    document.getElementById('eventCategoryModalTitle').innerHTML = '<i class="bi bi-tags"></i> Crear Categoría de Evento';
    document.getElementById('eventCategoryForm').reset();
    document.getElementById('eventCategoryEditId').value = '';
    new bootstrap.Modal(document.getElementById('eventCategoryModal')).show();
}

function editEventCategory(id, name) {
    document.getElementById('eventCategoryModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Categoría';
    document.getElementById('eventCategoryEditId').value = id;
    document.getElementById('eventCategoryName').value = name;
    new bootstrap.Modal(document.getElementById('eventCategoryModal')).show();
}

async function saveEventCategory() {
    const id = document.getElementById('eventCategoryEditId').value;
    const data = {
        name: document.getElementById('eventCategoryName').value
    };

    try {
        const token = localStorage.getItem('userToken');
        const url = id ? `${API_BASE_URL}/category/${id}` : `${API_BASE_URL}/category`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Error al guardar categoría');

        bootstrap.Modal.getInstance(document.getElementById('eventCategoryModal')).hide();
        showAlert(`Categoría ${id ? 'actualizada' : 'creada'} exitosamente`, 'success', 'eventCategoriesAlert');
        loadEventCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar la categoría', 'danger', 'eventCategoriesAlert');
    }
}

async function toggleEventCategoryStatus(id) {
    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/category/status/${id}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al cambiar estado');

        showAlert('Estado actualizado exitosamente', 'success', 'eventCategoriesAlert');
        loadEventCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al actualizar el estado', 'danger', 'eventCategoriesAlert');
    }
}

async function deleteEventCategory(id) {
    if (!confirm('¿Está seguro de eliminar esta categoría?')) return;

    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/category/logical/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al eliminar categoría');

        showAlert('Categoría eliminada exitosamente', 'success', 'eventCategoriesAlert');
        loadEventCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar la categoría', 'danger', 'eventCategoriesAlert');
    }
}

// ============================================
// CONSUMICIONES
// ============================================
let currentConsumptionId = null;
let consumptionCategories = [];

async function loadConsumptions() {
    try {
        const response = await fetch(`${API_BASE_URL}/consumption`);
        if (!response.ok) throw new Error('Error al cargar consumiciones');
        
        const consumptions = await response.json();
        renderConsumptions(consumptions);
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('consumptionsTableBody').innerHTML = `
            <tr><td colspan="8" class="text-center text-danger">Error al cargar consumiciones</td></tr>
        `;
    }
}

function renderConsumptions(consumptions) {
    const tbody = document.getElementById('consumptionsTableBody');
    
    if (consumptions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8">
                    <div class="empty-state">
                        <i class="bi bi-cup-straw"></i>
                        <p>No hay consumiciones registradas</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = consumptions.map(consumption => `
        <tr>
            <td>${consumption.id}</td>
            <td>
                ${consumption.imageUrl ? 
                    `<img src="${consumption.imageUrl}" class="event-image-thumb" alt="${consumption.name}">` :
                    '<div class="event-image-thumb bg-secondary d-flex align-items-center justify-content-center"><i class="bi bi-image text-white"></i></div>'
                }
            </td>
            <td><strong>${consumption.name}</strong></td>
            <td>${consumption.categoryId || 'N/A'}</td>
            <td>${consumption.description}</td>
            <td>$${parseFloat(consumption.price).toFixed(2)}</td>
            <td><span class="badge-status ${consumption.active ? 'badge-active' : 'badge-inactive'}">${consumption.active ? 'Activo' : 'Inactivo'}</span></td>
            <td class="action-buttons">
                <button class="btn btn-sm btn-primary" onclick="editConsumption(${consumption.id})" title="Editar">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteConsumption(${consumption.id})" title="Eliminar">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

async function loadConsumptionCategories() {
    try {
        const response = await fetch(`${API_BASE_URL}/consumption-category`);
        if (!response.ok) throw new Error('Error al cargar categorías');
        
        consumptionCategories = await response.json();
        
        // Llenar select del modal
        const select = document.getElementById('consumptionCategoryId');
        select.innerHTML = '<option value="">Seleccione una categoría</option>' +
            consumptionCategories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('');
        
        // Renderizar cards de categorías
        renderConsumptionCategories(consumptionCategories);
    } catch (error) {
        console.error('Error:', error);
    }
}

function renderConsumptionCategories(categories) {
    const container = document.getElementById('consumptionCategoriesContainer');
    
    if (categories.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="bi bi-grid-3x3"></i>
                    <p>No hay categorías de consumiciones registradas</p>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = categories.map(category => `
        <div class="col-md-4 col-lg-3 mb-3">
            <div class="card-custom text-center">
                <i class="bi bi-grid-fill" style="font-size: 3rem; color: #f5576c;"></i>
                <h5 class="mt-3 mb-2">${category.name}</h5>
                <span class="badge-status ${category.active ? 'badge-active' : 'badge-inactive'} mb-3">
                    ${category.active ? 'Activa' : 'Inactiva'}
                </span>
                <div class="d-flex justify-content-center gap-2">
                    <button class="btn btn-sm btn-primary" onclick="editConsumptionCategory(${category.id}, '${category.name}')">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-warning" onclick="toggleConsumptionCategoryStatus(${category.id})">
                        <i class="bi bi-toggle-on"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteConsumptionCategory(${category.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function openCreateConsumptionModal() {
    currentConsumptionId = null;
    document.getElementById('consumptionModalTitle').innerHTML = '<i class="bi bi-cup-straw"></i> Crear Nueva Consumición';
    document.getElementById('consumptionForm').reset();
    document.getElementById('consumptionId').value = '';
    new bootstrap.Modal(document.getElementById('consumptionModal')).show();
}

async function editConsumption(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/consumption/${id}`);
        if (!response.ok) throw new Error('Error al cargar consumición');
        
        const consumption = await response.json();
        currentConsumptionId = id;
        
        document.getElementById('consumptionModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Consumición';
        document.getElementById('consumptionId').value = consumption.id;
        document.getElementById('consumptionName').value = consumption.name;
        document.getElementById('consumptionCategoryId').value = consumption.categoryId;
        document.getElementById('consumptionDescription').value = consumption.description;
        document.getElementById('consumptionPrice').value = consumption.price;
        document.getElementById('consumptionImageUrl').value = consumption.imageUrl || '';
        
        new bootstrap.Modal(document.getElementById('consumptionModal')).show();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar los datos de la consumición', 'danger', 'consumptionsAlert');
    }
}

async function saveConsumption() {
    const consumptionData = {
        categoryId: parseInt(document.getElementById('consumptionCategoryId').value),
        name: document.getElementById('consumptionName').value,
        description: document.getElementById('consumptionDescription').value,
        price: parseFloat(document.getElementById('consumptionPrice').value),
        imageUrl: document.getElementById('consumptionImageUrl').value || null,
        active: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    };

    try {
        const token = localStorage.getItem('userToken');
        const url = currentConsumptionId ? `${API_BASE_URL}/consumption/${currentConsumptionId}` : `${API_BASE_URL}/consumption`;
        const method = currentConsumptionId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(consumptionData)
        });

        if (!response.ok) throw new Error('Error al guardar consumición');

        bootstrap.Modal.getInstance(document.getElementById('consumptionModal')).hide();
        showAlert(`Consumición ${currentConsumptionId ? 'actualizada' : 'creada'} exitosamente`, 'success', 'consumptionsAlert');
        loadConsumptions();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar la consumición', 'danger', 'consumptionsAlert');
    }
}

async function deleteConsumption(id) {
    if (!confirm('¿Está seguro de eliminar esta consumición?')) return;

    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/consumption/logical/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al eliminar consumición');

        showAlert('Consumición eliminada exitosamente', 'success', 'consumptionsAlert');
        loadConsumptions();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar la consumición', 'danger', 'consumptionsAlert');
    }
}

// ============================================
// CATEGORÍAS DE CONSUMICIONES
// ============================================
function openCreateConsumptionCategoryModal() {
    document.getElementById('consumptionCategoryModalTitle').innerHTML = '<i class="bi bi-grid-3x3"></i> Crear Categoría de Consumición';
    document.getElementById('consumptionCategoryForm').reset();
    document.getElementById('consumptionCategoryEditId').value = '';
    new bootstrap.Modal(document.getElementById('consumptionCategoryModal')).show();
}

function editConsumptionCategory(id, name) {
    document.getElementById('consumptionCategoryModalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Categoría';
    document.getElementById('consumptionCategoryEditId').value = id;
    document.getElementById('consumptionCategoryName').value = name;
    new bootstrap.Modal(document.getElementById('consumptionCategoryModal')).show();
}

async function saveConsumptionCategory() {
    const id = document.getElementById('consumptionCategoryEditId').value;
    const data = {
        name: document.getElementById('consumptionCategoryName').value
    };

    try {
        const token = localStorage.getItem('userToken');
        const url = id ? `${API_BASE_URL}/consumption-category/${id}` : `${API_BASE_URL}/consumption-category`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            const errorMessage = errorData?.message || 'Error al guardar categoría';
            throw new Error(errorMessage);
        }

        bootstrap.Modal.getInstance(document.getElementById('consumptionCategoryModal')).hide();
        showAlert(`Categoría ${id ? 'actualizada' : 'creada'} exitosamente`, 'success', 'consumptionCategoriesAlert');
        loadConsumptionCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert(error.message || 'Error al guardar la categoría', 'danger', 'consumptionCategoriesAlert');
    }
}

async function toggleConsumptionCategoryStatus(id) {
    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/consumption-category/status/${id}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al cambiar estado');

        showAlert('Estado actualizado exitosamente', 'success', 'consumptionCategoriesAlert');
        loadConsumptionCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al actualizar el estado', 'danger', 'consumptionCategoriesAlert');
    }
}

async function deleteConsumptionCategory(id) {
    if (!confirm('¿Está seguro de eliminar esta categoría?')) return;

    try {
        const token = localStorage.getItem('userToken');
        const response = await fetch(`${API_BASE_URL}/consumption-category/logical/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Error al eliminar categoría');

        showAlert('Categoría eliminada exitosamente', 'success', 'consumptionCategoriesAlert');
        loadConsumptionCategories();
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar la categoría', 'danger', 'consumptionCategoriesAlert');
    }
}

// ============================================
// INICIALIZACIÓN
// ============================================
document.addEventListener('DOMContentLoaded', function() {
    // Verificar acceso de admin
    if (!checkAdminAccess()) return;

    // Cargar datos iniciales
    loadEvents();
    loadEventCategories();
    loadConsumptions();
    loadConsumptionCategories();

    // Event listeners para tabs
    document.getElementById('events-tab').addEventListener('shown.bs.tab', loadEvents);
    document.getElementById('event-categories-tab').addEventListener('shown.bs.tab', loadEventCategories);
    document.getElementById('consumptions-tab').addEventListener('shown.bs.tab', loadConsumptions);
    document.getElementById('consumption-categories-tab').addEventListener('shown.bs.tab', loadConsumptionCategories);
});
