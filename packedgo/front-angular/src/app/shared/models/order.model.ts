/**
 * Interfaces para el sistema de órdenes y checkout multi-admin
 */

export interface MultiOrderCheckoutResponse {
  sessionId: string;
  totalAmount: number;
  sessionStatus: string; // PENDING, PARTIAL, COMPLETED
  expiresAt: string;
  totalOrders: number;
  paidOrders: number;
  totalPaid: number;
  totalPending: number;
  paymentGroups: PaymentGroup[];
  message: string;
}

export interface PaymentGroup {
  adminId: number;
  adminName?: string; // Opcional, puede ser agregado en el futuro
  orderNumber: string;
  orderId: number;
  amount: number;
  status: string; // PENDING, PAID
  paymentPreferenceId?: string; // ID de Mercado Pago
  qrUrl?: string; // URL del QR de Mercado Pago
  initPoint?: string; // URL de checkout de Mercado Pago
  items: OrderItem[];
}

export interface OrderItem {
  eventId: number;
  eventName: string;
  quantity: number;
  price: number;
  totalPrice: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  adminId: number;
  totalAmount: number;
  status: string;
  isPaid: boolean;
  createdAt: string;
  items: OrderItem[];
  sessionId?: string; // Si pertenece a una sesión multi-orden
}

export interface MultiOrderSession {
  sessionId: string;
  userId: number;
  cartId: number;
  totalAmount: number;
  sessionStatus: string;
  createdAt: string;
  updatedAt?: string;
  expiresAt: string;
  orders: Order[];
}

export interface PaymentPreference {
  preferenceId: string;
  qrUrl: string;
  initPoint: string;
  orderNumber: string;
  amount: number;
}

export interface CreatePaymentRequest {
  adminId: number;
  orderId: string;
  amount: number;
}
