export interface Event {
  id?: number;
  name: string;
  description: string;
  eventDate: string;
  lat: number;
  lng: number;
  maxCapacity: number;
  currentCapacity?: number;
  basePrice: number;
  imageUrl?: string;
  categoryId: number;
  status?: string;
  active?: boolean;
  totalPasses?: number;
  availablePasses?: number;
  soldPasses?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface EventCategory {
  id?: number;
  name: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConsumptionCategory {
  id?: number;
  name: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
