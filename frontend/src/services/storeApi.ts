import api from './api';

export interface ItemCatalogResponse {
    id: number;
    itemName: string;
    itemType?: string;
    type?: string;
    description?: string;
    costCoins: number;
    rarity?: string;
    imageUrl?: string;
    positionX?: number;
    positionY?: number;
    scale?: number;
    rotation?: number;
    unlockRequirement?: number;
    isAvailable?: boolean;
}

export interface BuyItemResponse {
    catalogItem: ItemCatalogResponse;
    newTotalCoins: number;
}

export interface GalaxyItem {
    id: number;
    userId: number;
    catalogItemId: number;
    purchaseDate: string;
}

export const storeApi = {
    getAvailableItems: async (): Promise<ItemCatalogResponse[]> => {
        const response = await api.get('/api/store/items');
        return response.data;
    },

    getMyItems: async (): Promise<GalaxyItem[]> => {
        const response = await api.get('/api/store/my-items');
        return response.data;
    },

    buyItem: async (itemId: number): Promise<BuyItemResponse> => {
        const response = await api.post(`/api/store/buy/${itemId}`);
        return response.data;
    }
};