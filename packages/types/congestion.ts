export type CongestionLevel = 'GREEN' | 'YELLOW' | 'ORANGE' | 'RED' | 'UNKNOWN';

export interface CongestionData {
    areaName: string;
    congestionLevel: CongestionLevel;
    congestionMessage: string;
    populationMin: number;
    populationMax: number;
    updateTime: string;
}